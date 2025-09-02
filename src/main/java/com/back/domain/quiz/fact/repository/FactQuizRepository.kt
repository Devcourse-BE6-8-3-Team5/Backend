package com.back.domain.quiz.fact.repository

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.quiz.fact.entity.FactQuiz
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface FactQuizRepository : JpaRepository<FactQuiz, Long> {
    // FactQuiz에서 RealNews.newsCategory 기반으로 조회 (N+1 문제 방지를 위해 JOIN FETCH 사용)
    @Query(
        """
            SELECT DISTINCT fq
            FROM FactQuiz fq
            JOIN FETCH fq.realNews rn
            WHERE rn.newsCategory = :category
            
            """
    )
    fun findByCategory(@Param("category") category: NewsCategory): List<FactQuiz>

    //
    @Query(
        """
            SELECT DISTINCT fq
            FROM FactQuiz fq
            JOIN FETCH fq.realNews
            
            """
    )
    fun findAllWithNews(): List<FactQuiz>

    @Query(
        """
            SELECT DISTINCT fq
            FROM FactQuiz fq
            JOIN FETCH fq.realNews
            JOIN FETCH fq.fakeNews
            WHERE fq.id = :id
            
            """
    )
    fun findByIdWithNews(@Param("id") id: Long): Optional<FactQuiz>

    @Query(
        """
                SELECT DISTINCT fq.realNews.id 
                FROM FactQuiz fq 
                WHERE fq.realNews.createdDate >= :start 
                  AND fq.realNews.createdDate < :end
            
            """
    )
    fun findRealNewsIdsWithFactQuiz(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Set<Long>

    //추가
    fun findByRealNewsId(realNewsId: Long): Optional<FactQuiz>
}
