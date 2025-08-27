package com.back.domain.news.real.repository;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.entity.RealNews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface RealNewsRepository extends JpaRepository<RealNews, Long> {

    // 제목 검색 (Fulltext) + N번째 제외 (카테고리별 랭킹 제외)
    @Query(value = """
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
    """,
                countQuery = """
    SELECT COUNT(*)
    FROM real_news rn
    WHERE rn.title_tsv @@ plainto_tsquery(:title)
      AND rn.id != :excludedId
    """,
            nativeQuery = true)
    Page<RealNews> findByTitleExcludingNthCategoryRank(
            @Param("title") String title,
            @Param("excludedId") Long excludedId,
            @Param("excludedRank") int excludedRank,
            Pageable pageable);

    // 전체 조회에서 카테고리별 N번째 제외
    @Query(value = """
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
    """,
                countQuery = """
    SELECT COUNT(*)
    FROM real_news rn
    WHERE rn.id != :excludedId
    """,
            nativeQuery = true)
    Page<RealNews> findAllExcludingNth(
            @Param("excludedId") Long excludedId,
            @Param("excludedRank") int excludedRank,
            Pageable pageable);

    // 카테고리별 조회에서 N번째 제외
    @Query(value = """
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
    """,
                countQuery = """
    SELECT COUNT(*)
    FROM real_news rn
    WHERE rn.news_category = :#{#category.name()}
      AND rn.id != :excludedId
    """,
            nativeQuery = true)
    Page<RealNews> findByCategoryExcludingNth(
            @Param("category") NewsCategory category,
            @Param("excludedId") Long excludedId,
            @Param("excludedRank") int excludedRank,
            Pageable pageable);

    // 모든 카테고리에서 N번째 순위 뉴스 조회
    @Query(value = """
    SELECT rn.*
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) AS rn
        FROM real_news
    ) AS sub
    JOIN real_news rn ON rn.id = sub.id
    WHERE sub.rn = :targetRank
    ORDER BY rn.created_date DESC
    """,
                nativeQuery = true)
        List<RealNews> findNthRankByAllCategories(@Param("targetRank") int targetRank);

        // 특정 카테고리에서 N번째 순위 뉴스 조회
        @Query(value = """
    SELECT rn.*
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (ORDER BY created_date DESC) AS rn
        FROM real_news
        WHERE news_category = :#{#category.name()}
    ) AS sub
    JOIN real_news rn ON rn.id = sub.id
    WHERE sub.rn = :targetRank
    """,
            nativeQuery = true)
    Optional<RealNews> findNthRankByCategory(
            @Param("category") NewsCategory category,
            @Param("targetRank") int targetRank);

    // 기본 전체 조회 (인덱스: idx_real_news_created_date_desc 직접 활용)
    Page<RealNews> findAllByOrderByCreatedDateDesc(Pageable pageable);

    // 카테고리별 조회 (인덱스: idx_real_news_category_created_date 직접 활용)
    Page<RealNews> findByNewsCategoryOrderByCreatedDateDesc(NewsCategory category, Pageable pageable);
    // ID 제외 조회들 (정렬 포함)
    Page<RealNews> findByIdNotOrderByCreatedDateDesc(Long excludedId, Pageable pageable);
    Page<RealNews> findByNewsCategoryAndIdNotOrderByCreatedDateDesc(NewsCategory category, Long excludedId, Pageable pageable);
    Page<RealNews> findByTitleContainingAndIdNotOrderByCreatedDateDesc(String title, Long excludedId, Pageable pageable);

    // 날짜 범위 조회 - 정렬 추가로 인덱스 활용 개선
    @Query("SELECT rn FROM RealNews rn WHERE rn.createdDate BETWEEN :start AND :end ORDER BY rn.createdDate DESC")
    List<RealNews> findByCreatedDateBetweenOrderByCreatedDateDesc(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}

