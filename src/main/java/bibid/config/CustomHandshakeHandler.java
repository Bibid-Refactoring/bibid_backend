package bibid.config;

import bibid.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.WebUtils;

import javax.annotation.Nullable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Map;

/**
 * WebSocket 연결 시 JWT 기반 인증을 처리하는 HandshakeHandler입니다.
 * HTTP 요청에서 쿠키로 전달된 ACCESS_TOKEN을 검증하고,
 * 인증된 사용자 정보를 Principal로 반환하여 WebSocket 연결에 보안 컨텍스트를 부여합니다.
 */
@Slf4j
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtProvider jwtProvider;

    // JwtProvider를 생성자 주입받아 토큰 검증에 사용합니다.
    public CustomHandshakeHandler(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    /**
     * WebSocket 연결 시 사용자의 인증 정보를 결정하는 메서드.
     *
     * @param request    WebSocket 연결 요청을 감싼 HTTP 요청 객체 (Spring의 추상화 타입)
     * @param wsHandler  실제 WebSocket 메시지를 처리할 핸들러 인스턴스
     * @param attributes WebSocket 세션에 값을 저장할 수 있는 속성 맵 (예: 유저 ID, 토큰 등)
     * @return Principal 인증된 사용자 정보 (없을 경우 null 또는 super 처리)
     */
    @Override
    @Nullable
    protected Principal determineUser(
            @Nullable ServerHttpRequest request,
            @Nullable WebSocketHandler wsHandler,
            @Nullable Map<String, Object> attributes) {

        // 1단계: 모든 파라미터 null 방어 (IDE 경고 제거)
        if (request == null || wsHandler == null || attributes == null) {
            log.warn("determineUser: received null parameter(s), returning null");
            return null;
        }

        // 2단계: 타입 확인 후 다운캐스팅
        // instanceof A a: 객체가 A 타입인지 검사하면서 동시에 변수 a로 자동 다운캐스팅까지 해주는 Java의 패턴 매칭 기능
        if (!(request instanceof ServletServerHttpRequest servletRequestWrapper)) {
            log.warn("determineUser: not a ServletServerHttpRequest");
            return super.determineUser(request, wsHandler, attributes);
        }

        HttpServletRequest servletRequest = servletRequestWrapper.getServletRequest();

        // 3단계: 쿠키에서 ACCESS_TOKEN 추출
        Cookie jwtCookie = WebUtils.getCookie(servletRequest, "ACCESS_TOKEN");
        if (jwtCookie != null) {
            String token = jwtCookie.getValue();
            log.debug("JWT cookie received");

            try {
                String username = jwtProvider.validateAndGetSubject(token);
                log.debug("Username extracted: {}", username);
                attributes.put("ACCESS_TOKEN", token);
                return new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                return super.determineUser(request, wsHandler, attributes);
            }
        } else {
            log.warn("No ACCESS_TOKEN found in cookie");
            return super.determineUser(request, wsHandler, attributes);
        }
    }
}