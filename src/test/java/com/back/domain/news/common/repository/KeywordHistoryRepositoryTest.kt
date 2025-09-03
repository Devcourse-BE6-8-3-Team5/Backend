package com.back.domain.news.common.repository

import com.back.domain.news.common.entity.KeywordHistory
import com.back.domain.news.common.enums.KeywordType
import com.back.domain.news.common.enums.NewsCategory
import com.back.global.jpa.JpaConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@Import(KeywordHistoryRepositoryImpl::class, JpaConfig::class) // Custom 구현체 및 JpaConfig 등록
class KeywordHistoryRepositoryTest {

    @Autowired private lateinit var keywordHistoryRepository: KeywordHistoryRepository
    @Autowired private lateinit var entityManager: EntityManager

    private lateinit var testData: MutableList<KeywordHistory>

    @BeforeEach
    fun setUp() {
        testData = mutableListOf<KeywordHistory>().apply {
            // 카테고리별로 3개씩 생성 (TITLE 타입, 날짜 다르게 분산)
            NewsCategory.entries.forEach { category ->
                repeat(3) { idx ->
                    add(
                        KeywordHistory(
                            keyword = "${category.name}_키워드${idx + 1}",
                            keywordType = KeywordType.GENERAL,
                            category = category,
                            usedDate = LocalDate.now().minusDays(idx.toLong())
                        )
                    )
                }
            }
            // 특별히 많이 등장하는 키워드 추가 (임계값 검증용)
            repeat(5) {
                add(
                    KeywordHistory(
                        keyword = "중복키워드",
                        keywordType = KeywordType.BREAKING,
                        category = NewsCategory.ECONOMY,
                        usedDate = LocalDate.now()
                    )
                )
            }
        }

        testData.forEach { entityManager.persist(it) }
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("특정 기간 이후 임계값 이상 사용된 키워드 조회")
    fun findOverusedKeywords() {
        val result = keywordHistoryRepository.findOverusedKeywords(LocalDate.now().minusDays(2), 3)
        assertThat(result).contains("중복키워드")
    }

    @Test
    @DisplayName("특정 날짜에 사용된 키워드 조회")
    fun findKeywordsByUsedDate() {
        val todayKeywords = keywordHistoryRepository.findKeywordsByUsedDate(LocalDate.now())
        assertThat(todayKeywords).contains("중복키워드")
    }

    @Test
    @DisplayName("특정 날짜 이전 키워드 삭제")
    fun deleteByUsedDateBefore() {
        val cutoffDate = LocalDate.now().minusDays(1)
        val deleted = keywordHistoryRepository.deleteByUsedDateBefore(cutoffDate)

        // 각 카테고리별로 하루 이상 지난 키워드들이 삭제됨
        assertThat(deleted).isGreaterThan(0)
    }

    @Test
    @DisplayName("키워드 목록, 카테고리, 사용일자로 조회")
    fun findByKeywordsAndCategoryAndUsedDate() {
        val today = LocalDate.now()
        val result = keywordHistoryRepository.findByKeywordsAndCategoryAndUsedDate(
            keywords = listOf("중복키워드", "ECONOMY_키워드1"),
            category = NewsCategory.ECONOMY,
            usedDate = today
        )
        assertThat(result).isNotEmpty
        assertThat(result.map { it.category }.toSet()).containsOnly(NewsCategory.ECONOMY)
        assertThat(result.map { it.usedDate }.toSet()).containsOnly(today)
    }
}