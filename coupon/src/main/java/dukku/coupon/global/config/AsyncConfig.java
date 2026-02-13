package dukku.coupon.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "logTaskExecutor")
    public Executor logTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 핵심 스레드 수: 로그 기록은 I/O 작업이 아니며 큐 삽입일 뿐이므로 CPU 코어 수에 맞게 설정
        executor.setCorePoolSize(8);
        // 최대 스레드 수
        executor.setMaxPoolSize(16);
        // 큐 용량: 대량의 로그 요청을 수용할 수 있도록 충분히 설정
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("LogExecutor-");
        // 애플리케이션 종료 시 큐에 남은 작업 처리 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
