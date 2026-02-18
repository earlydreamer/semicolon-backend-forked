package dukku.common.global.config;

import dukku.common.global.logging.mdc.MdcTaskDecorator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final MdcTaskDecorator mdcTaskDecorator;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.initialize();
        return executor;
    }
}
