package com.back.domain.quiz.fact.eventListener

import com.back.domain.news.fake.event.FakeNewsCreatedEvent
import com.back.domain.quiz.fact.service.FactQuizService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class FactQuizEventListener(
    private val factQuizService: FactQuizService
) {
    private val log = LoggerFactory.getLogger(FactQuizEventListener::class.java)

    @EventListener
    fun handleFakeNewsCreated(event: FakeNewsCreatedEvent) {
        val realNewsIds = event.realNewsIds

        if (realNewsIds.isNullOrEmpty()) {
            return  // 처리할 뉴스가 없으면 종료
        }

        try {
            factQuizService.create(realNewsIds)
        } catch (e: Exception) {
            log.error("팩트 퀴즈 생성 중 오류 발생: {}", e.message, e)
        }
    }
}
