package bibid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {
    // JavaMailSender Bean 등록: 이메일 발송에 사용됨
    @Bean
    public JavaMailSender MailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Gmail SMTP 서버 설정
        mailSender.setHost("smtp.gmail.com"); // SMTP 서버 호스트
        mailSender.setPort(587);              // TLS 포트 번호

        // 발신자 이메일 계정 설정 (실제 Gmail 계정)
        mailSender.setUsername("bibidcrown@gmail.com");       // SMTP 인증용 이메일
        mailSender.setPassword("fgukxychnfrcbrvs");           // 앱 비밀번호 또는 OAuth 토큰

        // SMTP 프로퍼티 설정
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");         // 전송 프로토콜
        props.put("mail.smtp.auth", "true");                  // 인증 사용
        props.put("mail.smtp.starttls.enable", "true");       // TLS 사용
        props.put("mail.debug", "false");                      // 디버깅 로그 출력

        return mailSender; // 메일 전송기 Bean 반환
    }
}