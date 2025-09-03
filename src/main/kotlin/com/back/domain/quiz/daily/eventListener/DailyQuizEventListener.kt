package com.back.domain.quiz.daily.eventListener

import com.back.domain.news.today.event.TodayNewsCreatedEvent
import com.back.domain.quiz.daily.service.DailyQuizService
import com.back.domain.quiz.detail.event.DetailQuizCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DailyQuizEventListener(
    private val dailyQuizService: DailyQuizService
) {
    private val log = LoggerFactory.getLogger(DailyQuizEventListener::class.java)

    @Async("dailyQuizExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleTodayNewsCreated(event: TodayNewsCreatedEvent) {
        try {
            dailyQuizService.createDailyQuiz(event.todayNewsId)
        } catch (e: Exception) {
            log.error("오늘의 퀴즈 생성 중 오류 발생: {}", e.message, e)
        }
    }

    @Async("dailyQuizExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleDetailQuizCreated(event: DetailQuizCreatedEvent) {
        try {
            // dailyNewsProcess() 메서드로 생성된 오늘의 뉴스에 대한 퀴즈를 생성(상세 퀴즈 생성 후)
            dailyQuizService.createDailyQuiz()
        } catch (e: Exception) {
            log.error("오늘의 퀴즈 생성 중 오류 발생: {}", e.message, e)
        }
    }
}
