package com.back.domain.news.today.repository

import com.back.domain.news.today.entity.TodayNews
import java.time.LocalDate

interface TodayNewsRepositoryCustom {
    // N+1 문제 해결을 위한 연관관계 포함 조회 메서드들
    fun findQBySelectedDateWithDetailQuizzes(selectedDate: LocalDate): TodayNews?
    fun findQByIdWithDetailQuizzes(id: Long): TodayNews?
    fun findQTopByOrderBySelectedDateDescWithDetailQuizzes(): TodayNews?
}