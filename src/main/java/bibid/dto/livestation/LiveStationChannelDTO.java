package bibid.dto.livestation;

import bibid.entity.LiveStationChannel;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LiveStationChannelDTO {

    private Long liveStationChannelIndex;

    // YouTube RTMP 송출용 주소
    private String youtubeStreamUrl;

    // YouTube 스트림 키
    private String youtubeStreamKey;

    // YouTube 라이브 시청용 URL
    private String youtubeWatchUrl;

    // 채널 사용 가능 여부
    private boolean isAvailable;

    // 현재 채널이 사용 중인지 여부
    private boolean isAllocated;

    // YouTube 방송 ID (transition API 호출 시 사용됨)
    private String youtubeBroadcastId;

    // YouTube 스트림 ID
    private String youtubeStreamId;

    public LiveStationChannel toEntity() {
        return LiveStationChannel.builder()
                .liveStationChannelIndex(this.liveStationChannelIndex)
                .youtubeStreamUrl(this.youtubeStreamUrl)
                .youtubeStreamKey(this.youtubeStreamKey)
                .youtubeWatchUrl(this.youtubeWatchUrl)
                .isAvailable(this.isAvailable)
                .isAllocated(this.isAllocated)
                .youtubeBroadcastId(this.youtubeBroadcastId)
                .youtubeStreamId(this.youtubeStreamId)
                .build();
    }
}
