package com.back.domain.news.common.service

import com.back.domain.news.common.dto.KeywordGenerationResDto
import com.back.domain.news.common.dto.KeywordWithType
import com.back.domain.news.common.entity.KeywordHistory
import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.common.repository.KeywordHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate


@Service
class KeywordHistoryService(private val keywordHistoryRepository: KeywordHistoryRepository) {

    @Transactional
    fun saveKeywords(keywords: KeywordGenerationResDto, usedDate: LocalDate) {
        saveKeywordsByCategory(keywords.society, NewsCategory.SOCIETY, usedDate)
        saveKeywordsByCategory(keywords.economy, NewsCategory.ECONOMY, usedDate)
        saveKeywordsByCategory(keywords.politics, NewsCategory.POLITICS, usedDate)
        saveKeywordsByCategory(keywords.culture, NewsCategory.CULTURE, usedDate)
        saveKeywordsByCategory(keywords.it, NewsCategory.IT, usedDate)
    }


    private fun saveKeywordsByCategory(
        keywords: List<KeywordWithType>,
        category: NewsCategory,
        usedDate: LocalDate
    ) {
        val keywordStrings = keywords.map { it.keyword }

        val existingKeywords = keywordHistoryRepository.findByKeywordsAndCategoryAndUsedDate(
            keywordStrings, category, usedDate
        )

        val existingMap = existingKeywords.associateBy { it.keyword}

        val keywordHistories = keywords.map { keyword ->
            existingMap[keyword.keyword]?.apply {
                incrementUseCount()
            } ?: KeywordHistory(
                keyword = keyword.keyword,
                keywordType = keyword.keywordType,
                category = category,
                usedDate = usedDate
            )
        }
        keywordHistoryRepository.saveAll(keywordHistories)
    }

    // 1. 최근 5일간 3회 이상 사용된 키워드 (과도한 반복 방지)
    @Transactional(readOnly = true)
    fun getOverusedKeywords(days: Int, minUsage: Int): List<String> {
        val startDate = LocalDate.now().minusDays(days.toLong())

        return keywordHistoryRepository.findOverusedKeywords(startDate, minUsage)
    }

    @Transactional(readOnly = true)
    fun getYesterdayKeywords(): List<String>{
        val yesterday = LocalDate.now().minusDays(1)

        return keywordHistoryRepository.findKeywordsByUsedDate(yesterday)
    }

    fun getRecentKeywords(recentDays: Int): List<String> {
        val startDate = LocalDate.now().minusDays(recentDays.toLong())
        val histories = keywordHistoryRepository.findByUsedDateGreaterThanEqual(startDate)

        return histories.map { it.keyword }.distinct()
    }
}
