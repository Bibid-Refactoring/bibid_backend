package bibid.config;

import bibid.entity.LiveStationChannel;
import bibid.repository.livestation.LiveStationChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class YoutubeChannelInitializer implements CommandLineRunner {

    private final LiveStationChannelRepository channelRepository;

    @Override
    public void run(String... args) {
        if (channelRepository.count() == 0) {
            LiveStationChannel channel = LiveStationChannel.builder()
                    .youtubeStreamUrl("rtmp://a.rtmp.youtube.com/live2")
                    .youtubeStreamKey("05gj-15rf-dzp8-8pmq-843r")
                    .youtubeWatchUrl("")
                    .isAllocated(false)
                    .isAvailable(true)
                    .build();

            channelRepository.save(channel);
        }
    }
}