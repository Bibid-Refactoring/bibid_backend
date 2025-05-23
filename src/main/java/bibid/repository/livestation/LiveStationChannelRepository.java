package bibid.repository.livestation;

import bibid.entity.LiveStationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LiveStationChannelRepository extends JpaRepository<LiveStationChannel, Long> {

    // YouTube 스트림 키로 채널 조회
    Optional<LiveStationChannel> findByYoutubeStreamKey(String youtubeStreamKey);

    // YouTube 시청 URL로 채널 조회
    Optional<LiveStationChannel> findByYoutubeWatchUrl(String youtubeWatchUrl);

    // YouTube 송출 주소로 채널 조회
    Optional<LiveStationChannel> findByYoutubeStreamUrl(String youtubeStreamUrl);
}
