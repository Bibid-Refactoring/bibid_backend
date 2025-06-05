package bibid.dto.livestation;

import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveStream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class YoutubeBroadcastResult {
    private LiveBroadcast broadcast;
    private LiveStream stream;
}
