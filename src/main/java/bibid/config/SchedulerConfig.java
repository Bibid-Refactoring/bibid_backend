package bibid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    // 경매 스케줄링 전용 TaskScheduler 빈 등록
    @Bean(name = "auctionTaskScheduler")  // 명시적 이름 부여로 다른 Scheduler와 구분 가능
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // 동시에 최대 10개 작업 스레드 실행 가능
        scheduler.setThreadNamePrefix("auction-task-"); // 스레드 이름 prefix 설정 (디버깅용)
        scheduler.initialize();
        return scheduler;
    }
}

