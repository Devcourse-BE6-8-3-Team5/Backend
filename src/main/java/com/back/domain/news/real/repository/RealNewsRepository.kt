package com.back.domain.news.real.repository

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.entity.RealNews
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface RealNewsRepository : JpaRepository<RealNews?, Long?> {
    // 제목 검색 (Fulltext) + N번째 제외 (카테고리별 랭킹 제외)
    @Query(
        value = """
    WITH ranked AS (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) AS category_rank
        FROM real_news
        WHERE title_tsv @@ plainto_tsquery(:title)
          AND id != :excludedId
    )
    SELECT rn.*
    FROM real_news rn
    WHERE rn.title_tsv @@ plainto_tsquery(:title)
      AND rn.id != :excludedId
      AND NOT EXISTS (
          SELECT 1
          FROM ranked
          WHERE ranked.id = rn.id
            AND ranked.category_rank = :excludedRank
      )
    ORDER BY rn.created_date DESC
    
    """.trimIndent(), countQuery = """
    SELECT COUNT(*)
    FROM real_news rn
    WHERE rn.title_tsv @@ plainto_tsquery(:title)
      AND rn.id != :excludedId
    
    """.trimIndent(), nativeQuery = true
    )
    fun findByTitleExcludingNthCategoryRank(
        @Param("title") title: String?,
        @Param("excludedId") excludedId: Long?,
        @Param("excludedRank") excludedRank: Int,
        pageable: Pageable?
    ): Page<RealNews?>?

    // 전체 조회에서 카테고리별 N번째 제외
    @Query(
        value = """
    WITH ranked AS (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) AS category_rank
        FROM real_news
        WHERE id != :excludedId
    )
    SELECT rn.*
    FROM real_news rn
    WHERE rn.id != :excludedId
      AND NOT EXISTS (
          SELECT 1 FROM ranked
          WHERE ranked.id = rn.id
            AND ranked.category_rank = :excludedRank
      )
    ORDER BY rn.created_date DESC
    
    """.trimIndent(), countQuery = """
    SELECT COUNT(*)
    FROM real_news rn
    WHERE rn.id != :excludedId
    
    """.trimIndent(), nativeQuery = true
    )
    fun findAllExcludingNth(
        @Param("excludedId") excludedId: Long?,
        @Param("excludedRank") excludedRank: Int,
        pageable: Pageable?
    ): Page<RealNews?>?

    // 카테고리별 조회에서 N번째 제외
    @Query(
        value = """
    WITH ranked_news AS (
        SELECT id,
               ROW_NUMBER() OVER (ORDER BY created_date DESC) AS rank_num
        FROM real_news
        WHERE news_category = :#{#category.name()}
          AND id != :excludedId
    )
    SELECT rn.*
    FROM real_news rn
    WHERE rn.news_category = :#{#category.name()}
      AND rn.id != COALESCE((
          SELECT r.id
          FROM ranked_news r
          WHERE r.rank_num = :excludedRank
      ), 0)
    ORDER BY rn.created_date DESC
    
    """.trimIndent(), countQuery = """
    SELECT COUNT(*)
    FROM real_news rn
    WHERE rn.news_category = :#{#category.name()}
      AND rn.id != :excludedId
    
    """.trimIndent(), nativeQuery = true
    )
    fun findByCategoryExcludingNth(
        @Param("category") category: NewsCategory?,
        @Param("excludedId") excludedId: Long?,
        @Param("excludedRank") excludedRank: Int,
        pageable: Pageable?
    ): Page<RealNews?>?

    // 모든 카테고리에서 N번째 순위 뉴스 조회
    @Query(
        value = """
    SELECT rn.*
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) AS rn
        FROM real_news
    ) AS sub
    JOIN real_news rn ON rn.id = sub.id
    WHERE sub.rn = :targetRank
    ORDER BY rn.created_date DESC
    
    """.trimIndent(), nativeQuery = true
    )
    fun findNthRankByAllCategories(@Param("targetRank") targetRank: Int): MutableList<RealNews?>?

    // 특정 카테고리에서 N번째 순위 뉴스 조회
    @Query(
        value = """
    SELECT rn.*
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (ORDER BY created_date DESC) AS rn
        FROM real_news
        WHERE news_category = :#{#category.name()}
    ) AS sub
    JOIN real_news rn ON rn.id = sub.id
    WHERE sub.rn = :targetRank
    
    """.trimIndent(), nativeQuery = true
    )
    fun findNthRankByCategory(
        @Param("category") category: NewsCategory?,
        @Param("targetRank") targetRank: Int
    ): Optional<RealNews?>?

    // 기본 전체 조회 (인덱스: idx_real_news_created_date_desc 직접 활용)
    fun findAllByOrderByCreatedDateDesc(pageable: Pageable?): Page<RealNews?>?

    // 카테고리별 조회 (인덱스: idx_real_news_category_created_date 직접 활용)
    fun findByNewsCategoryOrderByCreatedDateDesc(category: NewsCategory?, pageable: Pageable?): Page<RealNews?>?

    // ID 제외 조회들 (정렬 포함)
    fun findByIdNotOrderByCreatedDateDesc(excludedId: Long?, pageable: Pageable?): Page<RealNews?>?
    fun findByNewsCategoryAndIdNotOrderByCreatedDateDesc(
        category: NewsCategory?,
        excludedId: Long?,
        pageable: Pageable?
    ): Page<RealNews?>?

    fun findByTitleContainingAndIdNotOrderByCreatedDateDesc(
        title: String?,
        excludedId: Long?,
        pageable: Pageable?
    ): Page<RealNews?>?

    // 날짜 범위 조회 - 정렬 추가로 인덱스 활용 개선
    @Query("SELECT rn FROM RealNews rn WHERE rn.createdDate BETWEEN :start AND :end ORDER BY rn.createdDate DESC")
    fun findByCreatedDateBetweenOrderByCreatedDateDesc(
        @Param("start") start: LocalDateTime?,
        @Param("end") end: LocalDateTime?
    ): MutableList<RealNews?>?
}

