package com.back.domain.quiz.detail.repository

import com.back.domain.quiz.detail.entity.DetailQuiz
import org.springframework.data.jpa.repository.JpaRepository

interface DetailQuizRepository : JpaRepository<DetailQuiz, Long> {
    fun findByRealNewsId(realNewsId: Long): List<DetailQuiz>

    fun deleteByRealNewsId(newsId: Long)
}
