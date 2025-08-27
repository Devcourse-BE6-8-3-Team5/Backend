package com.back.global.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "quizExecutor")
    public Executor quizExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 동시에 실행할 스레드 수
        executor.setMaxPoolSize(2);  // 최대 스레드 수
        executor.setQueueCapacity(50); // 대기 큐 크기
        executor.setThreadNamePrefix("QuizGen-");
        executor.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 모든 작업이 완료될 때까지 대기
        executor.setAwaitTerminationSeconds(30); // 종료 대기 시간
        executor.initialize();
        return executor;
    }

    @Bean(name = "newsExecutor")
    public Executor newsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3); // 동시에 실행할 스레드 수
        executor.setMaxPoolSize(6);  // 최대 스레드 수
        executor.setQueueCapacity(60); // 대기 큐 크기
        executor.setThreadNamePrefix("newsGen-");
        executor.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 모든 작업이 완료될 때까지 대기
        executor.setAwaitTerminationSeconds(180); // 종료 대기 시간
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setKeepAliveSeconds(300); // 5분간 유휴 시 스레드 정리
        executor.setAllowCoreThreadTimeOut(false); // 코어 스레드는 유지
        executor.initialize();
        return executor;
    }

    @Bean(name = "dailyQuizExecutor")
    public Executor dailyQuizExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); // 동시에 실행할 스레드 수
        executor.setMaxPoolSize(1);  // 최대 스레드 수
        executor.setQueueCapacity(50); // 대기 큐 크기
        executor.setThreadNamePrefix("DailyQuizGen-");
        executor.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 모든 작업이 완료될 때까지 대기
        executor.setAwaitTerminationSeconds(30); // 종료 대기 시간
        executor.initialize();
        return executor;
    }

}
