package com.back.domain.news.today.service

import com.back.domain.news.real.repository.RealNewsRepository
import com.back.domain.news.today.entity.TodayNews
import com.back.domain.news.today.event.TodayNewsCreatedEvent
import com.back.domain.news.today.repository.TodayNewsRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
class TodayNewsService(
    private val todayNewsRepository: TodayNewsRepository,
    private val realNewsRepository: RealNewsRepository,
    private val publisher: ApplicationEventPublisher,
) {
    fun isAlreadyTodayNews(id: Long): Boolean = todayNewsRepository.existsById(id)

    @Transactional
    fun setTodayNews(id: Long) {
        val realNews = realNewsRepository.findById(id).orElseThrow { IllegalArgumentException("해당 ID의 뉴스가 존재하지 않습니다. ID: $id") }

        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        todayNewsRepository.deleteBySelectedDate(today)

        val todayNews = TodayNews(
            selectedDate = today,
            realNews = realNews
        )

        val savedTodayNews = todayNewsRepository.save(todayNews)
        publisher.publishEvent(TodayNewsCreatedEvent(savedTodayNews.id))
    }

}