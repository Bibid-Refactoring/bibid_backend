package bibid.config;

import bibid.entity.LiveStationChannel;
import bibid.repository.livestation.LiveStationChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// 애플리케이션 시작 시 YouTube 채널 정보가 비어 있다면 기본 채널을 하나 생성
@Component
@RequiredArgsConstructor
public class YoutubeChannelInitializer implements CommandLineRunner {

    private final LiveStationChannelRepository channelRepository;

    @Override
    public void run(String... args) {
        if (channelRepository.count() == 0) {
            LiveStationChannel channel = LiveStationChannel.builder()
                    .youtubeStreamUrl("rtmp://a.rtmp.youtube.com/live2") // YouTube RTMP 서버 주소
                    .youtubeStreamKey("05gj-15rf-dzp8-8pmq-843r")        // 스트림 키
                    .youtubeWatchUrl("")                                 // 시청 URL은 이후 등록 예정
                    .isAllocated(false)                                  // 현재 할당되지 않음
                    .isAvailable(true)                                   // 사용 가능 상태
                    .build();

            channelRepository.save(channel);
        }
    }
}