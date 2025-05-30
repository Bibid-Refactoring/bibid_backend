package bibid.controller.specialAuction;

import bibid.dto.AuctionDto;
import bibid.dto.ResponseDto;
import bibid.entity.Auction;
import bibid.dto.livestation.LiveStationChannelDTO;
import bibid.entity.CustomUserDetails;
import bibid.entity.LiveStationChannel;
import bibid.entity.Member;
import bibid.repository.livestation.LiveStationChannelRepository;
import bibid.repository.specialAuction.SpecialAuctionRepository;
import bibid.service.specialAuction.SpecialAuctionService;
import bibid.service.specialAuction.impl.GoogleTokenProvider;
import bibid.service.specialAuction.impl.GoogleYoutubeService;
import bibid.service.specialAuction.impl.SpecialAuctionScheduler;
import com.google.api.services.youtube.model.LiveBroadcast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/specialAuction")
@RequiredArgsConstructor
@Slf4j
public class SpecialAuctionController {

    private final SpecialAuctionService specialAuctionService;
    private final SpecialAuctionRepository specialAuctionRepository;
    private final LiveStationChannelRepository channelRepository;
    private final SpecialAuctionScheduler specialAuctionScheduler;

    // ✅ 추가된 Google 연동 컴포넌트들
    private final GoogleYoutubeService googleYoutubeService;
    private final GoogleTokenProvider tokenProvider;

    @GetMapping("/list")
    public ResponseEntity<?> getAuctionsByType(
            @RequestParam("auctionType") String auctionType,
            @PageableDefault(page = 0, size = 100, sort = "moddate", direction = Sort.Direction.DESC) Pageable pageable) {

        // 응답 데이터를 담을 Map 선언
        ResponseDto<AuctionDto> responseDto = new ResponseDto<>();

        try {

            Page<AuctionDto> auctionDtoList = specialAuctionService.findAuctionsByType(auctionType, pageable);

            if (auctionDtoList.isEmpty()) {
                log.info("No auctions found for auctionType: {}", auctionType);
            } else {
                log.info("Found auctions: {}", auctionDtoList.getContent());
            }

            responseDto.setPageItems(auctionDtoList);
            responseDto.setStatusCode(HttpStatus.OK.value());
            responseDto.setStatusMessage("ok");

            return ResponseEntity.ok(responseDto);
        } catch(Exception e) {
            log.error("getAuctions error: {}", e.getMessage());
            responseDto.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDto.setStatusMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(responseDto);
        }
    }

    // ✅ 라이브 방송 종료
    @PostMapping("/endLive/{auctionIndex}")
    public ResponseEntity<?> endLive(@PathVariable("auctionIndex") Long auctionIndex) {
        Auction auction = specialAuctionRepository.findById(auctionIndex)
                .orElseThrow(() -> new RuntimeException("해당 옥션은 없습니다."));

        LiveStationChannel channel = auction.getLiveStationChannel();
        if (channel == null || channel.getYoutubeBroadcastId() == null) {
            return ResponseEntity.badRequest().body("YouTube 방송 정보가 없습니다.");
        }

        try {
            String accessToken = tokenProvider.getAccessToken();
            googleYoutubeService.transitionBroadcastToComplete(channel.getYoutubeBroadcastId(), accessToken);

            auction.setAuctionStatus("종료됨");
            specialAuctionRepository.save(auction);

            return ResponseEntity.ok("YouTube 라이브 방송이 종료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("방송 종료 실패: " + e.getMessage());
        }
    }

    // ✅ 라이브 방송 시작
    @PostMapping("/startLive/{auctionIndex}")
    public ResponseEntity<?> startLive(@PathVariable("auctionIndex") Long auctionIndex) {
        Auction auction = specialAuctionRepository.findById(auctionIndex)
                .orElseThrow(() -> new RuntimeException("해당 옥션은 없습니다."));

        LiveStationChannel channel = auction.getLiveStationChannel();
        if (channel == null || channel.getYoutubeBroadcastId() == null) {
            return ResponseEntity.badRequest().body("YouTube 방송 정보가 없습니다.");
        }

        try {
            String accessToken = tokenProvider.getAccessToken();
            googleYoutubeService.transitionBroadcastToLive(channel.getYoutubeBroadcastId(), accessToken);

            auction.setAuctionStatus("방송중");
            specialAuctionRepository.save(auction);

            return ResponseEntity.ok("YouTube 라이브 방송이 시작되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("방송 시작 실패: " + e.getMessage());
        }
    }

    // 채널 정보 요청
    @GetMapping("/channelInfo/{auctionIndex}")
    public ResponseEntity<LiveStationChannelDTO> getChannelInfo(@PathVariable("auctionIndex") Long auctionIndex) {

        Auction auction = specialAuctionRepository.findById(auctionIndex)
                .orElseThrow(() -> new RuntimeException("해당 경매를 찾을 수 없습니다."));

        LiveStationChannel channel = auction.getLiveStationChannel();

        if (channel == null) {
            throw new RuntimeException("경매에 연결된 YouTube 채널 정보가 없습니다.");
        }

        LiveStationChannelDTO dto = channel.toDto();

        log.info("채널 정보 요청 - auctionIndex={}, youtubeWatchUrl={}", auctionIndex, dto.getYoutubeWatchUrl());

        return ResponseEntity.ok(dto);
    }


    @PostMapping("/registerAlarm/{auctionIndex}")
    public ResponseEntity<ResponseDto<String>> registerAuctionAlarm(
            @PathVariable("auctionIndex") Long auctionIndex,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ResponseDto<String> responseDto = new ResponseDto<>();
        Member member = userDetails.getMember();
        Auction auction = specialAuctionRepository.findById(auctionIndex).orElseThrow(
                () -> new RuntimeException("해당 옥션은 없습니다.")
        );

        if (auction == null) {
            responseDto.setStatusCode(HttpStatus.NOT_FOUND.value());
            responseDto.setStatusMessage("경매를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDto);
        }

        boolean isRegistered = specialAuctionScheduler.registerAlarmForUser(auction, member.getMemberIndex());

        if (isRegistered) {
            responseDto.setStatusCode(HttpStatus.OK.value());
            responseDto.setStatusMessage("알림 신청이 완료되었습니다.");
            responseDto.setItem("알림 신청 성공");
            return ResponseEntity.ok(responseDto);
        } else {
            responseDto.setStatusCode(HttpStatus.CONFLICT.value());
            responseDto.setStatusMessage("이미 알림이 등록되어 있습니다.");
            responseDto.setItem("알림 등록 중복");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(responseDto);
        }
    }

    // ✅ 라이브 방송 생성
    @PostMapping("/createLive/{auctionIndex}")
    public ResponseEntity<?> createLive(@PathVariable("auctionIndex") Long auctionIndex) {
        try {
            Auction auction = specialAuctionRepository.findById(auctionIndex)
                    .orElseThrow(() -> new RuntimeException("경매를 찾을 수 없습니다."));

            String accessToken = tokenProvider.getAccessToken();

            String title = auction.getProductName();
            String description = auction.getProductDescription();
            String scheduledTime = auction.getStartingLocalDateTime().toString(); // ISO 8601

            LiveBroadcast broadcast = googleYoutubeService.insertBroadcastAndBindStream(
                    title, description, scheduledTime, accessToken
            );

            LiveStationChannel channel = auction.getLiveStationChannel();
            if (channel != null) {
                channel.setYoutubeWatchUrl("https://www.youtube.com/watch?v=" + broadcast.getId());
                channel.setYoutubeBroadcastId(broadcast.getId());
                channelRepository.save(channel);
            }

            return ResponseEntity.ok("방송 생성 성공");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("방송 생성 실패: " + e.getMessage());
        }
    }
}
