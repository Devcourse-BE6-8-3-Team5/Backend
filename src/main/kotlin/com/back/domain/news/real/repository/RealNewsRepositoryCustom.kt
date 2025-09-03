package com.back.domain.news.real.repository

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.entity.RealNews
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface RealNewsRepositoryCustom {

    // 기존 Native Query를 QueryDSL로 변환한 메서드들
    fun findQByTitleExcludingNthCategoryRank(
        title: String,
        excludedId: Long,
        excludedRank: Int,
        pageable: Pageable
    ): Page<RealNews>

    fun findQAllExcludingNth(
        excludedId: Long,
        excludedRank: Int,
        pageable: Pageable
    ): Page<RealNews>

    fun findQByCategoryExcludingNth(
        category: NewsCategory,
        excludedId: Long,
        excludedRank: Int,
        pageable: Pageable
    ): Page<RealNews>

    fun findQNthRankByAllCategories(targetRank: Int): List<RealNews>

    fun findQNthRankByCategory(
        category: NewsCategory,
        targetRank: Int
    ): RealNews?


}