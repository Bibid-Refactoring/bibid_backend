package bibid.service.specialAuction.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;

@Component
@RequiredArgsConstructor
public class GoogleTokenProvider {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.refresh.token}")
    private String refreshToken;

    @Value("${google.api.token.url}")
    private String tokenUrl;

    private String currentAccessToken;
    private Instant accessTokenExpiryTime;

    public String getAccessToken() {
        // 이미 유효하면 재사용
        if (currentAccessToken != null && Instant.now().isBefore(accessTokenExpiryTime.minusSeconds(60))) {
            return currentAccessToken;
        }

        try {
            HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
            GenericUrl url = new GenericUrl(tokenUrl);

            Map<String, String> params = Map.of(
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "refresh_token", refreshToken,
                    "grant_type", "refresh_token"
            );

            HttpContent content = new UrlEncodedContent(params);
            HttpRequest request = requestFactory.buildPostRequest(url, content);
            HttpResponse response = request.execute();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode json = objectMapper.readTree(response.getContent());

            currentAccessToken = json.get("access_token").asText();
            int expiresIn = json.get("expires_in").asInt();
            accessTokenExpiryTime = Instant.now().plusSeconds(expiresIn);

            return currentAccessToken;
        } catch (IOException e) {
            throw new RuntimeException("Google access token 갱신 실패", e);
        }
    }
}

