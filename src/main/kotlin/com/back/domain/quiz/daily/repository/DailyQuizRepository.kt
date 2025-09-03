package com.back.domain.quiz.daily.repository

import com.back.domain.news.today.entity.TodayNews
import com.back.domain.quiz.daily.entity.DailyQuiz
import com.back.domain.quiz.detail.entity.DetailQuiz
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DailyQuizRepository : JpaRepository<DailyQuiz, Long> {
    @Query(
        """
            SELECT dq 
            FROM DailyQuiz dq
            JOIN FETCH dq.detailQuiz 
            WHERE dq.todayNews.id = :todayNewsId
            
            """
    )
    fun findByTodayNewsId(@Param("todayNewsId") todayNewsId: Long): List<DailyQuiz>

    fun existsByTodayNews(todayNews: TodayNews): Boolean

    fun existsByDetailQuiz(quiz: DetailQuiz): Boolean
}
