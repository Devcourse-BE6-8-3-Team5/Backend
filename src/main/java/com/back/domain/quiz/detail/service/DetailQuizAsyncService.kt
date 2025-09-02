package com.back.domain.quiz.detail.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Service
class DetailQuizAsyncService(
    private val detailQuizService: DetailQuizService,
    private val detailQuizRateLimitedService: DetailQuizRateLimitedService
) {
    private val inProgressMap = ConcurrentHashMap<Long, Boolean>()

    @Async("quizExecutor")
    fun generateAsync(newsId: Long): CompletableFuture<Void> {
        if (inProgressMap.putIfAbsent(newsId, java.lang.Boolean.TRUE) != null) {
            log.warn("이미 진행 중인 퀴즈 생성 작업이 있습니다. 뉴스 ID: $newsId")
            return CompletableFuture.completedFuture(null) // 이미 진행 중인 작업이 있으면 바로 반환
        }

        return try {
            // Rate limit 적용하여 AI 호출 후 퀴즈 생성
            val quizzes = detailQuizRateLimitedService.generatedQuizzesWithRateLimit(newsId)

            // 생성된 퀴즈 DB 저장
            detailQuizService.saveQuizzes(newsId, quizzes)

            log.info("상세 퀴즈 생성 완료, 뉴스 ID: $newsId")
            CompletableFuture.completedFuture(null)
        } catch (e: Exception) {
            log.error("[실패] 뉴스 퀴즈 생성 실패 - newsId: {}, 오류: {}", newsId, e.message, e)
            CompletableFuture.completedFuture(null)
        } finally {
            inProgressMap.remove(newsId)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DetailQuizAsyncService::class.java)
    }

}
