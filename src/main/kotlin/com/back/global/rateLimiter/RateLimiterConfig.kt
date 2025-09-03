package com.back.global.rateLimiter

import io.github.bucket4j.Bucket
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class RateLimiterConfig {
    @Bean
    fun bucket(): Bucket {
        return Bucket.builder()
            .addLimit { limit ->
                limit.capacity(12).refillIntervally(1, Duration.ofSeconds(5))
            }
            .build()
    }
}
