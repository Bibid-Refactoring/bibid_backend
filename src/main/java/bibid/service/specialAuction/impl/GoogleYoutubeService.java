package bibid.service.specialAuction.impl;

import bibid.dto.livestation.YoutubeBroadcastResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.CdnSettings;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;
import com.google.api.services.youtube.model.LiveStream;
import com.google.api.services.youtube.model.LiveStreamSnippet;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.util.DateTime;

@Service
public class GoogleYoutubeService {

    private final RestTemplate restTemplate;

    public GoogleYoutubeService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public void transitionBroadcastToLive(String broadcastId, String accessToken) {
        String url = "https://www.googleapis.com/youtube/v3/liveBroadcasts/transition" +
                "?part=status&id=" + broadcastId + "&broadcastStatus=live";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }

    public void transitionBroadcastToComplete(String broadcastId, String accessToken) {
        String url = "https://www.googleapis.com/youtube/v3/liveBroadcasts/transition" +
                "?part=status&id=" + broadcastId + "&broadcastStatus=complete";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }

    public String createLiveBroadcast(String title, String description, String accessToken) {
        try {
            HttpRequestFactory requestFactory = new NetHttpTransport()
                    .createRequestFactory(request -> request.getHeaders().setAuthorization("Bearer " + accessToken));

            GenericUrl url = new GenericUrl("https://www.googleapis.com/youtube/v3/liveBroadcasts?part=snippet,status,contentDetails");

            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> snippet = new HashMap<>();
            snippet.put("title", title);
            snippet.put("description", description);
            snippet.put("scheduledStartTime", Instant.now().plusSeconds(60).toString()); // 1분 후 시작

            Map<String, Object> status = new HashMap<>();
            status.put("privacyStatus", "public");

            Map<String, Object> contentDetails = new HashMap<>();
            contentDetails.put("monitorStream", Map.of("enableMonitorStream", false));

            payload.put("snippet", snippet);
            payload.put("status", status);
            payload.put("contentDetails", contentDetails);

            HttpContent content = new JsonHttpContent(new JacksonFactory(), payload);

            HttpRequest request = requestFactory.buildPostRequest(url, content);
            HttpResponse response = request.execute();

            Map<String, Object> jsonResponse = new ObjectMapper().readValue(response.getContent(), Map.class);
            return (String) jsonResponse.get("id"); // 방송 ID

        } catch (IOException e) {
            throw new RuntimeException("방송 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public YoutubeBroadcastResult insertBroadcastAndBindStream(
            String title, String description, String scheduledStartTime, String accessToken) throws IOException {

        YouTube youtube = getYoutube(accessToken);

        // 1. 방송 생성
        LiveBroadcast broadcast = new LiveBroadcast()
                .setSnippet(new LiveBroadcastSnippet()
                        .setTitle(title)
                        .setDescription(description)
                        .setScheduledStartTime(new DateTime(scheduledStartTime)))
                .setStatus(new LiveBroadcastStatus().setPrivacyStatus("public"))
                .setKind("youtube#liveBroadcast");

        LiveBroadcast insertedBroadcast = youtube.liveBroadcasts()
                .insert("snippet,status,contentDetails", broadcast)
                .execute();

        // 2. 스트림 생성
        LiveStream stream = new LiveStream()
                .setSnippet(new LiveStreamSnippet().setTitle(title + " 스트림"))
                .setCdn(new CdnSettings()
                        .setFormat("1080p")
                        .setResolution("1080p")
                        .setFrameRate("30fps")
                        .setIngestionType("rtmp"))
                .setKind("youtube#liveStream");

        LiveStream insertedStream = youtube.liveStreams()
                .insert("snippet,cdn", stream)
                .execute();

        // 3. 바인딩
        youtube.liveBroadcasts()
                .bind(insertedBroadcast.getId(), "id,snippet")
                .setStreamId(insertedStream.getId())
                .execute();

        return new YoutubeBroadcastResult(insertedBroadcast, insertedStream);
    }

    public YouTube getYoutube(String accessToken) {
        try {
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            HttpRequestInitializer initializer = request -> {
                request.getHeaders().setAuthorization("Bearer " + accessToken);
            };

            return new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, initializer)
                    .setApplicationName("bibid-auction")
                    .build();

        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("YouTube 초기화 실패: " + e.getMessage(), e);
        }
    }

    public void deleteBroadcast(String broadcastId, String accessToken) {
        try {
            YouTube youtube = getYoutube(accessToken);

            youtube.liveBroadcasts()
                    .delete(broadcastId)
                    .execute();

        } catch (IOException e) {
            throw new RuntimeException("방송 삭제 실패: " + e.getMessage(), e);
        }
    }

    public void deleteStream(String streamId, String accessToken) {
        try {
            YouTube youtube = getYoutube(accessToken);

            youtube.liveStreams()
                    .delete(streamId)
                    .execute();

        } catch (IOException e) {
            throw new RuntimeException("스트림 삭제 실패: " + e.getMessage(), e);
        }
    }

}


