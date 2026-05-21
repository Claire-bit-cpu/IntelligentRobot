package com.example.intelligentxtsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 * 用于飞书消息的异步处理，防止慢操作（AI问答、代码审查）导致webhook超时
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("messageExecutor")
    public Executor messageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("msg-");
        executor.setRejectedExecutionHandler((r, e) ->
                org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                        .warn("消息处理队列已满，拒绝任务"));
        executor.initialize();
        return executor;
    }
}
