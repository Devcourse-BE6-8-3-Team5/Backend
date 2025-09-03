package com.back.global.rateLimiter

import io.github.bucket4j.Bucket
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class RateLimiter(
    @Qualifier("bucket") private val bucket: Bucket
) {
    companion object {
        private val log = LoggerFactory.getLogger(RateLimiter::class.java)
        private const val MAX_WAIT_TIME = 60_000L
        private const val WAIT_INTERVAL = 2_000L
    }

    @Throws(InterruptedException::class)
    fun waitForRateLimit() {
        var attempts = 0
        val startTime = System.currentTimeMillis()

        while (!bucket.tryConsume(1)) {
            attempts++

            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime > MAX_WAIT_TIME) {
                throw RuntimeException("Rate limit 대기 시간 1분 초과")
            }

            log.debug("Rate limit 대기 중... 시도 횟수: {}", attempts)
            Thread.sleep(WAIT_INTERVAL)

            if (attempts % 10 == 0) {
                log.warn("Rate limit 대기가 길어지고 있습니다. 대기 횟수: {}", attempts)
            }
        }

        if (attempts > 0) {
            log.debug("Rate limit 토큰 획득 - 대기 횟수: {}", attempts)
        }
    }
}