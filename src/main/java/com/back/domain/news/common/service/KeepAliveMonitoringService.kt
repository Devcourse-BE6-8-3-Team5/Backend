package com.back.domain.news.common.service


import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.concurrent.ScheduledFuture

@Service
class KeepAliveMonitoringService(
    private val restTemplate: RestTemplate,
    private val taskScheduler: TaskScheduler,
    @Value("\${healthchecks.url}") private val healthcheckUrl: String
) {
    @Volatile
    private var keepAliveTask: ScheduledFuture<*>? = null

    companion object {
        private val log = LoggerFactory.getLogger(KeepAliveMonitoringService::class.java)
    }

    fun startBatchKeepAlive() {
        pingHealthcheck()
        startKeepAlive()
        log.info("Batch keep-alive started")
    }

    fun stopBatchKeepAlive() {
        stopKeepAlive()
        pingHealthcheck()
        log.info("Batch keep-alive stopped")
    }

    fun pingHealthcheck() {
        runCatching {
            restTemplate.getForObject(healthcheckUrl, String::class.java)
            log.info("Health checks ping sent: {}", healthcheckUrl)
        }.getOrElse { e ->
            log.warn("Health checks ping 실패: {}", healthcheckUrl, e)
        }
    }

    fun startKeepAlive() {
        keepAliveTask?.let { task ->
            log.warn("Keep-alive가 이미 실행 중입니다.")
            return
        }
        keepAliveTask = taskScheduler.scheduleAtFixedRate({ pingHealthcheck() }, Duration.ofMinutes(3))
    }

    fun stopKeepAlive() {
        keepAliveTask?.let { task ->
            val cancelled = task.cancel(true)
            log.info("Keep-alive stop requested - cancelled: {}", cancelled)
        }
        keepAliveTask = null
        log.info("Keep-alive stopped")
    }
}
