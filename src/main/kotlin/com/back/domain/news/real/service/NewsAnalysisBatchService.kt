package com.back.domain.news.real.service

import com.back.domain.news.common.dto.AnalyzedNewsDto
import com.back.domain.news.real.dto.RealNewsDto
import com.back.global.ai.AiService
import com.back.global.ai.processor.NewsAnalysisProcessor
import com.back.global.rateLimiter.RateLimiter
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture


@Service
class NewsAnalysisBatchService(
    private val aiService: AiService,
    private val objectMapper: ObjectMapper,
    private val rateLimiter: RateLimiter
) {
    companion object {
        private val log = LoggerFactory.getLogger(NewsAnalysisBatchService::class.java)
    }

    @Async("newsExecutor")
    fun processBatchAsync(batch: List<RealNewsDto>): CompletableFuture<List<AnalyzedNewsDto>> {
        log.info("스레드: ${Thread.currentThread().name}, 배치 시작")

        return runCatching {
            rateLimiter.waitForRateLimit()

            val processor = NewsAnalysisProcessor(batch.toMutableList(), objectMapper)
            val result = aiService.process(processor)

            log.info("스레드: ${Thread.currentThread().name}, 배치 완료 - ${result.size}개")
            result
        }.fold(
            onSuccess = { completedFuture(it) },
            onFailure = { e ->log.error("배치 처리 실패", e)
                completedFuture(emptyList())
            }
        )
    }
}