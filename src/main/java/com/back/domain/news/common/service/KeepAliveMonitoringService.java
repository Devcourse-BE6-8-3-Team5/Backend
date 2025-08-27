package com.back.domain.news.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeepAliveMonitoringService {
    private final RestTemplate restTemplate;
    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> keepAliveTask;

    @Value("${healthchecks.url}")
    private String healthcheckUrl;

    public void startBatchKeepAlive(){
        pingHealthcheck();
        startKeepAlive();
        log.info("Batch keep-alive started");
    }

    public void stopBatchKeepAlive() {
        stopKeepAlive();
        pingHealthcheck();
        log.info("Batch keep-alive stopped");
    }

    public void pingHealthcheck() {
        try {
            restTemplate.getForObject(healthcheckUrl, String.class);
            log.info("Health checks ping sent: {}", healthcheckUrl);
        } catch (Exception e) {
            log.warn("Health checks ping 실패: {}", healthcheckUrl, e);
        }
    }

    public void startKeepAlive() {
        if (keepAliveTask != null && !keepAliveTask.isCancelled()) {
            log.warn("Keep-alive가 이미 실행 중입니다.");
            return;
        }
        keepAliveTask = taskScheduler.scheduleAtFixedRate(this::pingHealthcheck, Duration.ofMinutes(3));
    }

    public void stopKeepAlive() {
        if (keepAliveTask != null) {
            keepAliveTask.cancel(true);
            keepAliveTask = null;
        }
        log.info("Keep-alive stopped");
    }
}
