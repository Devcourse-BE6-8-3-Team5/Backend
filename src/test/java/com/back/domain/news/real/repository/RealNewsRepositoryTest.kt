package com.back.domain.news.real.repository


import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.entity.RealNews
import com.back.global.jpa.JpaConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@Import(RealNewsRepositoryImpl::class, JpaConfig::class)
class RealNewsRepositoryTest
{
    @Autowired private lateinit var testEntityManager: TestEntityManager
    @Autowired private lateinit var realNewsRepository: RealNewsRepository
    private lateinit var testData: List<RealNews>

    @BeforeEach
    fun setUp() {
        // 테스트 데이터 생성 (각 카테고리별로 5개씩)
        realNewsRepository.deleteAll()
        testEntityManager.flush()
        testEntityManager.clear()

        // 테스트 데이터 생성 (각 카테고리별로 5개씩)
        val baseTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0) // 고정된 기준 시간 사용
        var timeOffset = 0L

        testData = mutableListOf<RealNews>().apply {
            NewsCategory.entries.forEach { category ->
                repeat(5) { index ->
                    add(createRealNews(
                        title = "${category.name} 뉴스 ${index + 1}",
                        category = category,
                        // 전체적으로 내림차순이 되도록 시간 설정
                        createdDate = baseTime.minusHours(timeOffset++)
                    ))
                }
            }
            // 검색 테스트용 특별한 제목의 뉴스 (가장 최근)
            add(createRealNews(
                title = "특별한 검색 제목",
                category = NewsCategory.entries.first(),
                createdDate = baseTime.plusHours(1) // 가장 최근 시간
            ))
        }

        // 데이터베이스에 저장
        testData.forEach { testEntityManager.persistAndFlush(it) }
        testEntityManager.flush()
        testEntityManager.clear()

        // 저장된 데이터를 실제 ID와 함께 다시 조회
        testData = realNewsRepository.findAllByOrderByCreatedDateDesc(PageRequest.of(0, 1000)).content
    }

    @Test
    @Order(1)
    @DisplayName("테스트 데이터 검증")
    fun verifyTestData() {
        // 각 카테고리별로 최소 5개 이상의 데이터가 있는지 확인
        NewsCategory.entries.forEach { category ->
            val count = realNewsRepository.countByNewsCategory(category)
            assertThat(count).isGreaterThanOrEqualTo(5)
        }
    }

    @Test
    @DisplayName("데이터 설정 검증")
    fun verifyTestDataSetup() {
        val categories = NewsCategory.entries

        categories.forEach { category ->
            val count = realNewsRepository.countByNewsCategory(category)
            println("Category: $category, Count: $count")

            if (count > 0) {
                val sample = realNewsRepository.findAllByNewsCategoryOrderByCreatedDateDesc(category, PageRequest.of(0, 3))
                sample.content.forEachIndexed { index, news ->
                    println("  ${index + 1}. ID: ${news.id}, Date: ${news.createdDate}")
                }
            }
        }
    }



    @Test
    @DisplayName("제목으로 검색하되 각 카테고리의 N번째 뉴스와 특정 ID 제외")
    fun findQByTitleExcludingNthCategoryRank() {
        // given
        val searchTitle = "뉴스"
        val excludedId = testData.first().id
        val excludedRank = 2
        val pageable = PageRequest.of(0, 10)

        // when
        val result = realNewsRepository.findQByTitleExcludingNthCategoryRank(
            searchTitle, excludedId, excludedRank, pageable
        )

        // then
        assertThat(result).isNotNull
        assertThat(result.content).isNotEmpty

        // 제목에 "뉴스"가 포함되어야 함
        result.content.forEach { news ->
            assertThat(news.title).containsIgnoringCase(searchTitle)
        }

        // excludedId는 결과에 포함되지 않아야 함
        assertThat(result.content.map { it.id }).doesNotContain(excludedId)

        // 각 카테고리의 2번째 뉴스들도 제외되어야 함 (실제 DB 데이터로 검증)
        val excludedIds = NewsCategory.entries.mapNotNull { category ->
            realNewsRepository.findByNewsCategoryAndIdNotOrderByCreatedDateDesc(category, excludedId, PageRequest.of(0, 100))
                .content
                .getOrNull(excludedRank - 1)?.id
        }

        val allExcludedIds = excludedIds + excludedId
        result.content.forEach { news ->
            assertThat(allExcludedIds).doesNotContain(news.id)
        }

        // 생성일 내림차순으로 정렬되어야 함
        val dates = result.content.map { it.createdDate }
        assertThat(dates).isSortedAccordingTo(Comparator.reverseOrder())
    }

    @Test
    @DisplayName("빈 제목으로 검색시 빈 결과 반환")
    fun findQByTitleExcludingNthCategoryRank_EmptyTitle() {
        // given
        val searchTitle = "존재하지않는제목"
        val excludedId = testData.first().id
        val excludedRank = 1
        val pageable = PageRequest.of(0, 10)

        // when
        val result = realNewsRepository.findQByTitleExcludingNthCategoryRank(
            searchTitle, excludedId, excludedRank, pageable
        )

        // then
        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isZero()
    }

    @Test
    @DisplayName("전체 뉴스 조회하되 각 카테고리의 N번째 뉴스와 특정 ID 제외")
    fun findQAllExcludingNth() {
        // given
        val excludedId = testData.first().id
        val excludedRank = 3
        val pageable = PageRequest.of(0, 20)

        // when
        val result = realNewsRepository.findQAllExcludingNth(excludedId, excludedRank, pageable)

        // then
        assertThat(result).isNotNull
        assertThat(result.content).isNotEmpty

        // excludedId는 결과에 포함되지 않아야 함
        assertThat(result.content.map { it.id }).doesNotContain(excludedId)

        // 실제 DB에서 각 카테고리별로 excludedId를 제외한 상태에서 3번째 뉴스 ID를 조회
        val categoryExcludedIds = NewsCategory.entries.mapNotNull { category ->
            realNewsRepository.findByNewsCategoryAndIdNotOrderByCreatedDateDesc(category, excludedId, PageRequest.of(0, 100))
                .content
                .getOrNull(excludedRank - 1)?.id
        }

        // 결과에는 excludedId와 각 카테고리의 N번째 뉴스들이 모두 제외되어야 함
        val allExcludedIds = categoryExcludedIds + excludedId
        result.content.forEach { news ->
            assertThat(allExcludedIds).doesNotContain(news.id)
        }

        // 생성일 내림차순으로 정렬되어야 함
        val dates = result.content.map { it.createdDate }
        assertThat(dates).isSortedAccordingTo(Comparator.reverseOrder())
    }

    @Test
    @DisplayName("페이징이 올바르게 동작하는지 확인")
    fun findQAllExcludingNth_Pagination() {
        // given
        val excludedId = testData.first().id
        val excludedRank = 1
        val pageSize = 5
        val firstPage = PageRequest.of(0, pageSize)
        val secondPage = PageRequest.of(1, pageSize)

        // when
        val firstResult = realNewsRepository.findQAllExcludingNth(excludedId, excludedRank, firstPage)
        val secondResult = realNewsRepository.findQAllExcludingNth(excludedId, excludedRank, secondPage)

        // then
        assertThat(firstResult.content).hasSize(pageSize)
        assertThat(firstResult.isFirst).isTrue()
        assertThat(firstResult.totalElements).isGreaterThan(pageSize.toLong())

        // 두 페이지의 내용이 겹치지 않아야 함
        val firstPageIds = firstResult.content.map { it.id }
        val secondPageIds = secondResult.content.map { it.id }
        assertThat(firstPageIds).doesNotContainAnyElementsOf(secondPageIds)
    }

    @Test
    @DisplayName("특정 카테고리에서 N번째 뉴스와 특정 ID 제외하고 조회")
    fun findQByCategoryExcludingNth() {
        // given
        val category = NewsCategory.entries.first()
        val excludedRank = 2
        val pageable = PageRequest.of(0, 10)

        // 실제 DB에서 해당 카테고리의 뉴스들을 조회하여 테스트 데이터로 사용
        val categoryNews = realNewsRepository.findAllByNewsCategoryOrderByCreatedDateDesc(category, PageRequest.of(0, 10))

        // 충분한 데이터가 있는지 확인
        assumeTrue(categoryNews.content.size >= 3) { "테스트를 위한 충분한 데이터가 없습니다." }

        val excludedId = categoryNews.content.first().id // 실제 DB의 첫 번째 뉴스 ID

        // when
        val result = realNewsRepository.findQByCategoryExcludingNth(
            category, excludedId, excludedRank, pageable
        )

        // then
        assertThat(result).isNotNull
        assertThat(result.content).isNotEmpty

        // 모든 결과가 해당 카테고리여야 함
        result.content.forEach { news ->
            assertThat(news.newsCategory).isEqualTo(category)
        }

        // excludedId는 결과에 포함되지 않아야 함
        assertThat(result.content.map { it.id }).doesNotContain(excludedId)

        // excludedId를 제외한 후 excludedRank번째 뉴스 ID 구하기
        val excludedNthId = realNewsRepository.findByNewsCategoryAndIdNotOrderByCreatedDateDesc(
            category, excludedId, PageRequest.of(0, excludedRank)
        ).content.getOrNull(excludedRank - 1)?.id

        // 두 ID 모두 결과에서 제외되어야 함
        if (excludedNthId != null) {
            assertThat(result.content.map { it.id }).doesNotContain(excludedNthId)
        }

        // 생성일 내림차순으로 정렬되어야 함
        val dates = result.content.map { it.createdDate }
        assertThat(dates).isSortedAccordingTo(Comparator.reverseOrder())

        // 페이징 검증
        assertThat(result.totalElements).isGreaterThan(0)
        assertThat(result.content.size).isLessThanOrEqualTo(pageable.pageSize)
    }


    @Test
    @DisplayName("모든 카테고리에서 N번째 뉴스들 조회")
    fun findQNthRankByAllCategories() {
        // given
        val targetRank = 2

        // when
        val result = realNewsRepository.findQNthRankByAllCategories(targetRank)

        // then
        assertThat(result).isNotNull

        // 각 카테고리별로 하나씩의 뉴스만 있어야 함
        val categoryCount = result.groupBy { it.newsCategory }.size
        assertThat(categoryCount).isLessThanOrEqualTo(NewsCategory.entries.size)

        // 각 카테고리별로 중복이 없어야 함
        val categories = result.map { it.newsCategory }
        assertThat(categories).doesNotHaveDuplicates()

        // 결과가 생성일 내림차순으로 정렬되어야 함
        val dates = result.map { it.createdDate }
        assertThat(dates).isSortedAccordingTo(Comparator.reverseOrder())

        // 각 결과가 해당 카테고리의 2번째 뉴스인지 확인 (실제 DB 데이터로 검증)
        // findQNthRankByAllCategories는 excludedId 조건 없이 순수하게 N번째를 조회하므로
        // 전체 조회 후 카테고리별로 필터링해서 검증
        result.forEach { news ->
            val allCategoryNews = realNewsRepository.findAllByOrderByCreatedDateDesc(PageRequest.of(0, 1000))
                .content
                .filter { it.newsCategory == news.newsCategory }
            val expectedNews = allCategoryNews.getOrNull(targetRank - 1)
            if (expectedNews != null) {
                assertThat(news.id).isEqualTo(expectedNews.id)
            }
        }
    }

    @Test
    @DisplayName("특정 카테고리에서 N번째 뉴스 조회")
    fun findQNthRankByCategory() {
        // given
        val category = NewsCategory.entries.first()
        val targetRank = 3

        // 먼저 해당 카테고리에 충분한 데이터가 있는지 확인
        val totalCount = realNewsRepository.countByNewsCategory(category)
        assumeTrue(totalCount >= targetRank) { "테스트를 위한 충분한 데이터가 없습니다. 필요: $targetRank, 실제: $totalCount" }

        // when
        val result = realNewsRepository.findQNthRankByCategory(category, targetRank)

        // then
        assertThat(result).isNotNull
        result?.let {
            assertThat(it.newsCategory).isEqualTo(category)

            // 구현체와 동일한 방식으로 검증: offset을 사용해서 해당 순위의 뉴스 조회
            val verificationData = realNewsRepository.findAllByNewsCategoryOrderByCreatedDateDesc(category, PageRequest.of(targetRank - 1, 1))

            if (verificationData.content.isNotEmpty()) {
                val expectedNews = verificationData.content.first()
                assertThat(result.id).isEqualTo(expectedNews.id)
                assertThat(result.createdDate).isEqualTo(expectedNews.createdDate)
            }
        }
    }

    @Test
    @DisplayName("존재하지 않는 랭킹 조회시 null 반환")
    fun findQNthRankByCategory_NotFound() {
        // given
        val category = NewsCategory.entries.first()
        val targetRank = 999 // 존재하지 않는 랭킹

        // when
        val result = realNewsRepository.findQNthRankByCategory(category, targetRank)

        // then
        assertThat(result).isNull()
    }

    private fun createRealNews(
        title: String,
        category: NewsCategory,
        createdDate: LocalDateTime = LocalDateTime.now()
    ): RealNews {
        return RealNews(
            content = "테스트 내용",
            title = title,
            description = "테스트 설명",
            link = "https://test.com/link",
            imgUrl = "https://test.com/image.jpg",
            originCreatedDate = createdDate,
            mediaName = "테스트 미디어",
            journalist = "테스트 기자",
            originalNewsUrl = "https://test.com/original",
            createdDate = createdDate,
            newsCategory = category
        )
    }
}