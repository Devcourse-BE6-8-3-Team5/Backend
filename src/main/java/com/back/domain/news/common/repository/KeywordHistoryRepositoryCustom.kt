package com.back.domain.news.common.repository

import com.back.domain.news.common.entity.KeywordHistory
import com.back.domain.news.common.enums.NewsCategory
import java.time.LocalDate

interface KeywordHistoryRepositoryCustom{

    fun findOverusedKeywords(startDate: LocalDate, threshold: Int): List<String>

    fun findKeywordsByUsedDate(date: LocalDate): List<String>

    fun deleteByUsedDateBefore(cutoffDate: LocalDate): Long

    fun findByKeywordsAndCategoryAndUsedDate(keywords: List<String>, category: NewsCategory, usedDate: LocalDate): List<KeywordHistory>

}
