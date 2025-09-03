package com.back.global.async

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
@EnableAsync
class AsyncConfig {
    @Bean("quizExecutor")
    fun quizExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2 // 동시에 실행할 스레드 수
            maxPoolSize = 2  // 최대 스레드 수
            queueCapacity = 50 // 대기 큐 크기
            threadNamePrefix = "QuizGen-"
            setWaitForTasksToCompleteOnShutdown(true) // 종료 시 모든 작업이 완료될 때까지 대기
            setAwaitTerminationSeconds(30) // 종료 대기 시간
            initialize()
        }
    }

    @Bean("newsExecutor")
    fun newsExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 3 // 동시에 실행할 스레드 수
            maxPoolSize = 6  // 최대 스레드 수
            queueCapacity = 60 // 대기 큐 크기
            threadNamePrefix = "newsGen-"
            setWaitForTasksToCompleteOnShutdown(true) // 종료 시 모든 작업이 완료될 때까지 대기
            setAwaitTerminationSeconds(180) // 종료 대기 시간
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            keepAliveSeconds = 300 // 5분간 유휴 시 스레드 정리
            setAllowCoreThreadTimeOut(false) // 코어 스레드는 유지
            initialize()
        }
    }

    @Bean("dailyQuizExecutor")
    fun dailyQuizExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 1 // 동시에 실행할 스레드 수
            maxPoolSize = 1  // 최대 스레드 수
            queueCapacity = 50 // 대기 큐 크기
            threadNamePrefix = "DailyQuizGen-"
            setWaitForTasksToCompleteOnShutdown(true) // 종료 시 모든 작업이 완료될 때까지 대기
            setAwaitTerminationSeconds(30) // 종료 대기 시간
            initialize()
        }
    }
}
