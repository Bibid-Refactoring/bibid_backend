package bibid.service.specialAuction.impl;

import bibid.dto.AccountUseHistoryDto;
import bibid.entity.*;
import bibid.repository.account.AccountRepository;
import bibid.repository.account.AccountUseHistoryRepository;
import bibid.repository.auction.AuctionRepository;
import bibid.service.notification.NotificationService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.Comparator;

@Component
@Slf4j
public class SpecialAuctionScheduler {

    private final SimpMessagingTemplate messagingTemplate;
    private final AuctionRepository auctionRepository;
    private final TaskScheduler taskScheduler;
    private final NotificationService notificationService;
    private final AccountRepository accountRepository;
    private final AccountUseHistoryRepository accountUseHistoryRepository;
    private final Map<Long, Map<Long, ScheduledFuture<?>>> scheduledNotifications = new ConcurrentHashMap<>();

    public SpecialAuctionScheduler(
            SimpMessagingTemplate messagingTemplate,
            AuctionRepository auctionRepository,
            @Qualifier("auctionTaskScheduler") TaskScheduler taskScheduler,
            NotificationService notificationService,
            AccountRepository accountRepository,
            AccountUseHistoryRepository accountUseHistoryRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.auctionRepository = auctionRepository;
        this.taskScheduler = taskScheduler;
        this.notificationService = notificationService;
        this.accountRepository = accountRepository;
        this.accountUseHistoryRepository = accountUseHistoryRepository;
    }

    /**
     * 경매 종료 시간에 맞춰 경매 처리 스케줄 등록
     */
    public void scheduleAuctionEnd(Long auctionIndex, LocalDateTime endingLocalDateTime) {
        Date endDate = Date.from(endingLocalDateTime.atZone(ZoneId.systemDefault()).toInstant());

        taskScheduler.schedule(() -> {
            log.info("경매 종료 스케줄 실행 - auctionIndex={}", auctionIndex);
            handleAuctionEnd(auctionIndex);
        }, endDate);

        log.info("경매 종료 스케줄 등록 완료 - auctionIndex={}, endDate={}", auctionIndex, endDate);
    }

    /**
     * 실제 경매 종료 및 낙찰자 처리
     */
    @Transactional
    public void handleAuctionEnd(Long auctionIndex) {
        try {
            Auction auction = auctionRepository.findByIdWithAllDetails(auctionIndex)
                    .orElseThrow(() -> new RuntimeException("경매를 찾을 수 없습니다. ID: " + auctionIndex));

            log.info("경매 조회 완료 - auctionIndex={}", auctionIndex);

            AuctionInfo lastBidInfo = auction.getAuctionInfoList()
                    .stream()
                    .max(Comparator.comparing(AuctionInfo::getBidTime))
                    .orElse(null);

            if (lastBidInfo != null) {
                // 낙찰 처리
                AuctionDetail auctionDetail = auction.getAuctionDetail();
                Member winner = lastBidInfo.getBidder();

                auctionDetail.setWinnerIndex(winner.getMemberIndex());
                auctionDetail.setWinningBid(lastBidInfo.getBidAmount());
                auctionDetail.setWinnerNickname(winner.getNickname());

                Account account = accountRepository.findByMember_MemberIndex(winner.getMemberIndex())
                        .orElseThrow(() -> new RuntimeException("낙찰자의 계좌 정보를 찾을 수 없습니다."));

                int balance = Integer.parseInt(account.getUserMoney());
                int bidAmount = lastBidInfo.getBidAmount().intValue();

                if (balance < bidAmount) {
                    throw new RuntimeException("낙찰자의 잔액이 부족합니다.");
                }

                account.setUserMoney(String.valueOf(balance - bidAmount));
                accountRepository.save(account);

                auction.setAuctionStatus("낙찰");

                // 입찰 히스토리 저장
                AccountUseHistoryDto historyDto = AccountUseHistoryDto.builder()
                        .auctionType("실시간 경매")
                        .accountIndex(account.getAccountIndex())
                        .beforeBalance(String.valueOf(balance))
                        .afterBalance(String.valueOf(balance - bidAmount))
                        .createdTime(LocalDateTime.now())
                        .productName(auction.getProductName())
                        .changeAccount(String.valueOf(bidAmount))
                        .useType("낙찰")
                        .memberIndex(winner.getMemberIndex())
                        .auctionIndex(auctionIndex)
                        .build();

                accountUseHistoryRepository.save(historyDto.toEntity(winner, auction, account));

                // 알림 전송
                notificationService.notifyAuctionWin(winner, auctionIndex);
                notificationService.notifyAuctionSold(auction.getMember(), lastBidInfo, auctionIndex);

                log.info("낙찰자 및 판매자 알림 전송 완료");
            } else {
                // 유찰 처리
                auction.setAuctionStatus("유찰");
                log.info("입찰 없음 - 유찰 처리 완료");
            }

            auctionRepository.save(auction);
            sendAuctionEndDetails(auction);

        } catch (Exception e) {
            log.error("경매 종료 처리 중 예외 발생 - auctionIndex={}, 오류={}", auctionIndex, e.getMessage(), e);
        }
    }

    /**
     * 사용자 개별 알림 예약 (경매 시작 10분 전)
     */
    @Transactional
    public boolean registerAlarmForUser(Auction auction, Long memberIndex) {
        Long auctionIndex = auction.getAuctionIndex();
        LocalDateTime notifyTime = auction.getStartingLocalDateTime().minusMinutes(10);

        // 중복 체크
        if (scheduledNotifications.containsKey(auctionIndex) &&
                scheduledNotifications.get(auctionIndex).containsKey(memberIndex)) {
            log.info("이미 알림 예약된 사용자 - auctionIndex={}, memberIndex={}", auctionIndex, memberIndex);
            return false;
        }

        Notification notification = notificationService.createScheduledNotification(auction, memberIndex);
        Long notificationIndex = notification.getNotificationIndex();

        ScheduledFuture<?> task = taskScheduler.schedule(
                () -> notificationService.sendAuctionStartNotificationToUser(auction, memberIndex, notificationIndex),
                Date.from(notifyTime.atZone(ZoneId.systemDefault()).toInstant())
        );

        scheduledNotifications
                .computeIfAbsent(auctionIndex, k -> new HashMap<>())
                .put(memberIndex, task);

        log.info("알림 예약 완료 - auctionIndex={}, memberIndex={}", auctionIndex, memberIndex);
        return true;
    }

    private void sendAuctionEndDetails(Auction auction) {
        messagingTemplate.convertAndSend("/topic/auction/" + auction.getAuctionIndex(), auction.getAuctionDetail().toDto());
        log.info("경매 종료 상세 정보 전송 완료 - auctionIndex={}", auction.getAuctionIndex());
    }
}
