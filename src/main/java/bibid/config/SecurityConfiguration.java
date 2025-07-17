package bibid.config;

import bibid.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

@Configuration
// Spring Security 보안 기능 활성화
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    // 커스텀 JWT 인증 필터: 요청에 담긴 토큰을 검사하여 인증 처리
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 비밀번호 암호화를 위한 Bean 등록 (BCrypt 사용)
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 보안 필터 체인 구성 (Spring Security의 핵심 설정)
    // CORS, CSRF 보호, HTTP Basic 인증, Stateless 방식, URL별 접근 권한 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(httpSecurityCorsConfigurer -> {
                    httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource());  // CORS 설정 적용
                })
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> {
                    authorizationManagerRequestMatcherRegistry.requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/members/**",
                            "/ws/**",
                            "/auction/**",
                            "/category/**",
                            "/api/ncloud/**",
                            "/mypage/**",
                            "/auth/**",
                            "/specialAuction/**",
                            "/specialAuction/list",
                            "/specialAuction/createLive",
                            "/auctionDetail/**",
                            "/ws-auctions/**",
                            "/ws-notifications/**",
                            "/api/banners/**"
                    ).permitAll();
                    authorizationManagerRequestMatcherRegistry.anyRequest().authenticated();
                })
                // JWT 인증 필터를 CORS 필터 다음에 등록
                // -> CORS가 먼저 처리되어야 브라우저에서 토큰 인증까지 도달할 수 있음
                .addFilterAfter(jwtAuthenticationFilter, CorsFilter.class)
                .build();
    }

    // CORS 설정 정의(어떤 Origin, Method, Header를 허용할지)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 도메인
        configuration.setAllowedOrigins(Arrays.asList("https://bibid.shop", "http://localhost:3000"));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 허용할 헤더
        configuration.setAllowedHeaders(Collections.singletonList("*"));

        // 쿠키 및 인증 정보를 포함한 요청 허용
        configuration.setAllowCredentials(true);

        // 모든 경로에 위 설정을 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
