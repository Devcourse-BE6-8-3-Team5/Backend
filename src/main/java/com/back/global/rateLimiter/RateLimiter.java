package com.back.global.rateLimiter;

import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RateLimiter {
    private final Bucket bucket;

    private static final long MAX_WAIT_TIME = 60000;
    private static final long WAIT_INTERVAL = 2000;

    public RateLimiter(@Qualifier("bucket") Bucket bucket) {
        this.bucket = bucket;
    }

    public void waitForRateLimit() throws InterruptedException {
        int attempts = 0;
        long startTime = System.currentTimeMillis();

        while (!bucket.tryConsume(1)) {
            attempts++;

            if (System.currentTimeMillis() - startTime > MAX_WAIT_TIME) {
                throw new RuntimeException("Rate limit 대기 시간 1분 초과");
            }

            log.debug("Rate limit 대기 중... 시도 횟수: {}", attempts);
            Thread.sleep(WAIT_INTERVAL); // 대기

            if (attempts % 10 == 0) {
                log.warn("Rate limit 대기가 길어지고 있습니다. 대기 횟수: {}", attempts);
            }
        }

        if (attempts > 0) {
            log.debug("Rate limit 토큰 획득 - 대기 횟수: {}", attempts);
        }
    }
}
