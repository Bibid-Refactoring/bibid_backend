package bibid.config;

import bibid.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.annotation.Nullable;
import java.util.ArrayList;

@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    // JwtProvider는 JWT 검증 및 subject(사용자 식별 정보) 추출을 담당하는 유틸리티 클래스입니다.
    public JwtChannelInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    // 클라이언트로부터 들어오는 메시지를 가로채기 위한 메서드
    @Override
    public Message<?> preSend(@Nullable Message<?> message, @Nullable MessageChannel channel) {
        // 메시지에서 STOMP 관련 정보를 추출하기 위한 accessor 생성
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 클라이언트가 WebSocket 연결 이후 STOMP CONNECT 명령을 보낼 때에만 인증 로직 수행
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // HandshakeHandler에서 미리 세션 속성(SessionAttributes)에 저장한 ACCESS_TOKEN을 꺼냄
            String token = (String) accessor.getSessionAttributes().get("ACCESS_TOKEN");

            // 토큰이 존재하고 비어있지 않은 경우
            if (token != null && !token.isEmpty()) {
                log.info("JwtChannelInterceptor - Found ACCESS_TOKEN in session attributes: {}", token);

                try {
                    // JWT 토큰의 유효성을 검증하고 subject(사용자 ID 혹은 이름)를 추출
                    String username = jwtProvider.validateAndGetSubject(token);
                    log.info("JwtChannelInterceptor - Extracted username from JWT: {}", username);

                    // 사용자 인증 정보를 STOMP 세션에 저장
                    // 여기서 세 번째 인자는 권한 목록인데, 현재는 비어 있는 상태로 설정
                    accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>()));

                    log.info("JwtChannelInterceptor - User set in WebSocket session: {}", username);
                } catch (Exception e) {
                    // JWT 검증 중 오류 발생 시 예외 발생 → WebSocket 연결 실패 처리됨
                    log.error("JwtChannelInterceptor - Invalid JWT Token", e);
                    throw new MessagingException("Invalid JWT Token", e);
                }
            } else {
                // 세션에 토큰이 존재하지 않으면 WebSocket 연결 거부
                log.warn("JwtChannelInterceptor - Missing JWT Token in WebSocket session attributes");
                throw new MessagingException("Missing JWT Token in WebSocket session attributes");
            }
        }

        // CONNECT 외의 메시지거나 인증이 정상적으로 완료되었으면 그대로 메시지를 반환하여 전송
        return message;
    }
}
