package com.back.domain.quiz.detail.service

import com.back.domain.quiz.detail.dto.DetailQuizDto
import com.back.global.exception.ServiceException
import io.github.bucket4j.Bucket
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DetailQuizRateLimitedService(
    private val detailQuizService: DetailQuizService,
    private val bucket: Bucket
) {
    @Throws(InterruptedException::class)
    fun generatedQuizzesWithRateLimit(newsId: Long): List<DetailQuizDto> {
        val maxRetries = 5 // 최대 재시도 횟수
        val retryDelay = 60000L // 재시도 대기 시간 (밀리초 단위)

        for (i in 0..<maxRetries) {
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit 제한으로 재시도 대기중... 시도 횟수: {} - newsId: {}", i + 1, newsId)
                Thread.sleep(retryDelay)
                continue
            }
            try {
                // Rate limit이 허용되면 AI 호출해 퀴즈 생성
                return detailQuizService.generateQuizzes(newsId)
            } catch (e: Exception) {
                log.warn("AI 호출 중 오류 발생. 재시도합니다. 시도 횟수: {} - newsId: {}, error: {}", i + 1, newsId, e.message)
                Thread.sleep(retryDelay)
            }
        }
        log.error("퀴즈 생성 최종 실패. 뉴스 ID: {}", newsId)
        throw ServiceException(500, "퀴즈 생성 최종 실패. 뉴스 ID: $newsId")
    }

    companion object {
        private val log = LoggerFactory.getLogger(DetailQuizRateLimitedService::class.java)
    }
}
