package com.back.domain.news.today.repository

import com.back.domain.news.today.entity.TodayNews
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

interface TodayNewsRepository : JpaRepository<TodayNews, Long> {
    @Modifying(clearAutomatically = true)
    @Transactional
    fun deleteBySelectedDate(today: LocalDate)

    fun findBySelectedDate(today: LocalDate): TodayNews?

    fun findTopByOrderBySelectedDateDesc(): TodayNews?
}
