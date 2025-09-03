package com.back.domain.quiz.detail.eventListener

import com.back.domain.news.real.event.RealNewsCreatedEvent
import com.back.domain.quiz.detail.service.DetailQuizEventService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class DetailQuizEventListener(
    private val detailQuizEventService: DetailQuizEventService
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @EventListener
    fun handleRealNewsCreated(event: RealNewsCreatedEvent) {
        log.info("RealNewsCreatedEvent 수신. 이벤트 발생: {}", event)

        val realNewsIds = event.realNewsIds
        try {
            detailQuizEventService.generateDetailQuizzes(realNewsIds)
        } catch (e: Exception) {
            log.error("상세 퀴즈 생성 중 오류 발생: {}", e.message, e)
        }
    }
}
