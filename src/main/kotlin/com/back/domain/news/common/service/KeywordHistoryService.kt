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
        val categoryMappings = mapOf(
            NewsCategory.SOCIETY to keywords.society,
            NewsCategory.ECONOMY to keywords.economy,
            NewsCategory.POLITICS to keywords.politics,
            NewsCategory.CULTURE to keywords.culture,
            NewsCategory.IT to keywords.it
        )
        
        categoryMappings.forEach { (category, keywordList) ->
            saveKeywordsByCategory(keywordList, category, usedDate)
        }
    }


    private fun saveKeywordsByCategory(
        keywords: List<KeywordWithType>,
        category: NewsCategory,
        usedDate: LocalDate
    ) {
        val keywordStrings = keywords.map { it.keyword }

        val existingKeywords = keywordHistoryRepository.findQByKeywordsAndCategoryAndUsedDate(
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

    @Transactional(readOnly = true)
    fun getMostRecentKeywords(): List<KeywordWithType> {
        return keywordHistoryRepository.findQMostRecentKeywords()
            .map { KeywordWithType(it.keyword, it.keywordType) }
    }

    @Transactional(readOnly = true)
    fun getExcludeKeywords(overuseDays: Int, overuseThreshold: Int): List<String> {
        // 과도하게 사용된 키워드만 제외
        return getOverusedKeywords(overuseDays, overuseThreshold)
    }

    // 1. 최근 5일간 3회 이상 사용된 키워드 (과도한 반복 방지)
    @Transactional(readOnly = true)
    fun getOverusedKeywords(days: Int, minUsage: Int): List<String> {
        val startDate = LocalDate.now().minusDays(days.toLong())

        return keywordHistoryRepository.findQOverusedKeywords(startDate, minUsage)
    }

}
