package com.back.domain.news.common.repository

import com.back.domain.news.common.entity.KeywordHistory
import com.back.domain.news.common.enums.NewsCategory
import java.time.LocalDate

interface KeywordHistoryRepositoryCustom{

    fun findQOverusedKeywords(startDate: LocalDate, threshold: Int): List<String>

    fun findQKeywordsByUsedDate(date: LocalDate): List<String>
    
    fun findQMostRecentKeywords(): List<KeywordHistory>

    fun deleteQByUsedDateBefore(cutoffDate: LocalDate): Long

    fun findQByKeywordsAndCategoryAndUsedDate(keywords: List<String>, category: NewsCategory, usedDate: LocalDate): List<KeywordHistory>

}
