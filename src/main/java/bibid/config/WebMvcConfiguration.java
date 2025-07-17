package bibid.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

// Spring Web MVC 관련 설정 클래스
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    
    // 정적 리소스 핸들러 설정
    // 어떤 URL 요청에 대해 어떤 리소스 위치를 제공할지 정의
    // 정적 파일 요청에 대한 캐시 시간 설정
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/templates/", "classpath:/static/")
                .setCacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES));
    }
}
