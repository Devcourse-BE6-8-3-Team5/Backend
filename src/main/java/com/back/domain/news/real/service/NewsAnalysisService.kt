package com.back.domain.news.real.service

import com.back.domain.news.common.dto.AnalyzedNewsDto
import com.back.domain.news.real.dto.RealNewsDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Service
import java.util.Collections.emptyList
import java.util.Collections.synchronizedList
import java.util.concurrent.CompletableFuture.allOf

@Service
class NewsAnalysisService(
    private val newsAnalysisBatchService: NewsAnalysisBatchService,
    @Value("\${news.filter.batch.size:2}") private val batchSize: Int
) {
    companion object {
        private val log = LoggerFactory.getLogger(NewsAnalysisService::class.java)
    }

    fun filterAndScoreNews(allRealNewsBeforeFilter: List<RealNewsDto>?): List<AnalyzedNewsDto> {
        if (allRealNewsBeforeFilter.isNullOrEmpty()) {
            log.warn("필터링할 뉴스가 없습니다.")
            return emptyList()
        }
        log.info("뉴스 필터링 시작 - 총 ${allRealNewsBeforeFilter.size}개")

        val batches = allRealNewsBeforeFilter.chunked(batchSize)
        val allResults = synchronizedList(mutableListOf<AnalyzedNewsDto>())

        val futures = batches.map { batch -> newsAnalysisBatchService.processBatchAsync(batch)
                .thenAccept { result -> allResults.addAll(result)
                    log.info("배치 완료 - 현재까지 {}개 처리됨", allResults.size)
                }
                .exceptionally { throwable -> log.error("배치 처리 실패", throwable)
                    if (isTaskRejectionException(throwable))
                        log.error("스레드풀 용량 부족 - 스레드 크기 확인 필요")
                    null
                }
        }

        return runCatching {
            allOf(*futures.toTypedArray()).join()
            allResults.toList()
        }.onFailure { e ->
            when (e.cause) {
                is InterruptedException -> {
                    Thread.currentThread().interrupt()
                    log.error("뉴스 분석 작업이 인터럽트됨", e.cause)
                }

                else -> log.error("원인 -> ", e.cause)
            }
        }.getOrElse {
            log.info("뉴스 필터링 완료 - 최종 결과: {}개", allResults.size)
            allResults.toList()
        }
    }

    // TaskRejectedException 감지
    private fun isTaskRejectionException(throwable: Throwable?): Boolean {
        var current = throwable
        while (current != null) {
            if (current is TaskRejectedException || current.javaClass.getSimpleName().contains("TaskReject"))
                return true

            current = current.cause
        }
        return false
    }
}
