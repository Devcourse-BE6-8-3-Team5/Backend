package com.back.domain.news.common.service

import com.back.domain.news.common.dto.KeywordGenerationResDto
import com.back.domain.news.common.dto.KeywordWithType
import com.back.domain.news.common.entity.KeywordHistory
import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.common.repository.KeywordHistoryRepository
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.function.Function
import java.util.stream.Collectors


@Service
@RequiredArgsConstructor
class KeywordHistoryService(private val keywordHistoryRepository: KeywordHistoryRepository) {

    @Transactional
    fun saveKeywords(keywords: KeywordGenerationResDto, usedDate: LocalDate) {
        // Save each keyword category to the repository

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

        val existingMap = existingKeywords.stream()
            .collect(
                Collectors.toMap(
                    Function { obj: KeywordHistory -> obj.keyword },
                    Function.identity<KeywordHistory>()
                )
            )

        // 4. 처리할 데이터 준비
        val keywordHistories: MutableList<KeywordHistory> = ArrayList()
        for (keyword in keywords) {
            val existing = existingMap.get(keyword.keyword)
            if (existing != null) {
                existing.incrementUseCount()
                keywordHistories.add(existing)
            } else {
                keywordHistories.add(
                    KeywordHistory(
                        keyword = keyword.keyword,
                        keywordType = keyword.keywordType,
                        category = category,
                        usedDate = usedDate
                    )
                )
            }
        }
        keywordHistoryRepository.saveAll<KeywordHistory>(keywordHistories)
    }

    // 1. 최근 5일간 3회 이상 사용된 키워드 (과도한 반복 방지)
    @Transactional(readOnly = true)
    fun getOverusedKeywords(days: Int, minUsage: Int): MutableList<String> {
        val startDate = LocalDate.now().minusDays(days.toLong())
        return keywordHistoryRepository!!.findOverusedKeywords(startDate, minUsage)
    }

    @get:Transactional(readOnly = true)
    val yesterdayKeywords: MutableList<String>
        // 2. 어제 사용된 키워드 중 일반적인 것들 (긴급 뉴스 제외)
        get() {
            val yesterday = LocalDate.now().minusDays(1)
            return keywordHistoryRepository.findKeywordsByUsedDate(yesterday)
        }

    fun getRecentKeywords(recentDays: Int): MutableList<String> {

        val startDate = LocalDate.now().minusDays(recentDays.toLong())
        val histories = keywordHistoryRepository.findByUsedDateGreaterThanEqual(startDate)

        return histories.stream()
            .map { obj: KeywordHistory -> obj.keyword }
            .distinct()
            .toList()
    }
}
