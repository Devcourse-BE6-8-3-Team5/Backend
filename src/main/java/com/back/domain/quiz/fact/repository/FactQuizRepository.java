package com.back.domain.quiz.fact.repository;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.quiz.fact.entity.FactQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FactQuizRepository extends JpaRepository<FactQuiz, Long> {

    // FactQuiz에서 RealNews.newsCategory 기반으로 조회 (N+1 문제 방지를 위해 JOIN FETCH 사용)
    @Query("""
            SELECT DISTINCT fq
            FROM FactQuiz fq
            JOIN FETCH fq.realNews rn
            WHERE rn.newsCategory = :category
            """)
    List<FactQuiz> findByCategory(@Param("category") NewsCategory category);

    //
    @Query("""
            SELECT DISTINCT fq
            FROM FactQuiz fq
            JOIN FETCH fq.realNews
            """)
    List<FactQuiz> findAllWithNews();

    @Query("""
            SELECT DISTINCT fq
            FROM FactQuiz fq
            JOIN FETCH fq.realNews
            JOIN FETCH fq.fakeNews
            WHERE fq.id = :id
            """)
    Optional<FactQuiz> findByIdWithNews(@Param("id") Long id);

    @Query("""
                SELECT DISTINCT fq.realNews.id 
                FROM FactQuiz fq 
                WHERE fq.realNews.createdDate >= :start 
                  AND fq.realNews.createdDate < :end
            """)
    Set<Long> findRealNewsIdsWithFactQuiz(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    //추가
    Optional<FactQuiz> findByRealNewsId(Long realNewsId);


}
