package com.back.domain.news.real.repository

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.entity.RealNews
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface RealNewsRepository : JpaRepository<RealNews, Long>, RealNewsRepositoryCustom {

    // 기본 전체 조회 (인덱스: idx_real_news_created_date_desc 직접 활용)
    fun findAllByOrderByCreatedDateDesc(pageable: Pageable): Page<RealNews>

    fun findByNewsCategoryAndIdNotOrderByCreatedDateDesc(
        category: NewsCategory,
        excludedId: Long,
        pageable: Pageable
    ): Page<RealNews>

    fun findByTitleContainingAndIdNotOrderByCreatedDateDesc(
        title: String,
        excludedId: Long,
        pageable: Pageable
    ): Page<RealNews>

    fun findByCreatedDateBetweenOrderByCreatedDateDesc(start: LocalDateTime, end: LocalDateTime): List<RealNews>

}