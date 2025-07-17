package bibid.config;

import bibid.jwt.JwtProvider;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket + STOMP 구성 클래스
 * JWT 인증 핸들러 적용
 * 경매 및 알림 채널에 WebSocket Endpoint 등록
 */
@Configuration
@EnableWebSocketMessageBroker // STOMP 기반 WebSocket 메시징 기능 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${front.url}")
    private String frontUrl;

    private final JwtProvider jwtProvider;

    // JwtProvider를 생성자 주입
    public WebSocketConfig(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    /**
     * 메시지 브로커 설정
     * 클라이언트가 구독할 수 있는 경로: /topic
     * 서버 수신 경로 prefix: /app
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // 예: /topic/auction, /topic/notification
        config.setApplicationDestinationPrefixes("/app"); // 예: /app/message
    }

    /**
     * 클라이언트가 WebSocket으로 연결할 수 있는 Endpoint 설정
     * JWT 기반 Handshake 처리
     * SockJS fallback 지원
     */
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // 공통 등록 메서드로 중복 제거
        registerEndpoint(registry, "/ws-auctions");
        registerEndpoint(registry, "/ws-notifications");
    }

    /**
     * WebSocket STOMP Endpoint 등록 메서드
     * JWT 핸드셰이크 핸들러 적용
     * origin 설정은 개발 단계에선 '*' 허용, 운영 시 frontUrl 적용 권장
     */
    private void registerEndpoint(StompEndpointRegistry registry, String endpoint) {
        registry.addEndpoint(endpoint)
                //.setAllowedOrigins(frontUrl + ":3000") // 운영 환경용
                .setAllowedOriginPatterns("*") // 개발 환경용 (보안상 운영 시 반드시 제한)
                .setHandshakeHandler(new CustomHandshakeHandler(jwtProvider))
                .withSockJS(); // SockJS fallback (WebSocket 지원 안 되는 브라우저 대응)
    }

    /**
     * 클라이언트로부터 수신되는 메시지 처리 채널에 Interceptor 추가
     * 메시지 송신 전 JWT 토큰 유효성 검사
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtChannelInterceptor(jwtProvider));
    }
}