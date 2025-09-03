package com.back.domain.quiz.fact.eventListener

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.fake.entity.FakeNews
import com.back.domain.news.fake.event.FakeNewsCreatedEvent
import com.back.domain.news.fake.repository.FakeNewsRepository
import com.back.domain.news.real.entity.RealNews
import com.back.domain.news.real.repository.RealNewsRepository
import com.back.domain.quiz.QuizType
import com.back.domain.quiz.fact.entity.FactQuiz
import com.back.domain.quiz.fact.repository.FactQuizRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@TestPropertySource(
    properties = ["NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "HEALTHCHECK_URL=health_check_url"
    ]
)
class FactQuizEventListenerTest {
    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var realNewsRepository: RealNewsRepository

    @Autowired
    private lateinit var fakeNewsRepository: FakeNewsRepository

    @Autowired
    private lateinit var factQuizRepository: FactQuizRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private var initialQuizCount = 0

    @BeforeEach
    fun setUp() {
        initialQuizCount = factQuizRepository.count().toInt()
    }

    @Test
    @DisplayName("FakeNewsCreatedEvent가 발행되면 팩트 퀴즈가 생성되어 DB에 저장됨")
    fun t1() {
        // given
        val realNews1 = createRealNews("content1", "title1")
        val realNews2 = createRealNews("content2", "title2")

        entityManager.persist(realNews1)
        entityManager.persist(realNews2)
        entityManager.flush()
        entityManager.clear()

        val managedRealNews1 = realNewsRepository.findById(realNews1.id).orElseThrow()
        val managedRealNews2 = realNewsRepository.findById(realNews2.id).orElseThrow()

        val fakeNews1 = createFakeNews(managedRealNews1, "fakeContent1")
        val fakeNews2 = createFakeNews(managedRealNews2, "fakeContent2")

        fakeNewsRepository.saveAll(listOf(fakeNews1, fakeNews2))

        managedRealNews1.fakeNews = fakeNews1
        managedRealNews2.fakeNews = fakeNews2

        val realNewsIds = listOf(managedRealNews1.id, managedRealNews2.id)

        // when
        val event = FakeNewsCreatedEvent(realNewsIds)
        eventPublisher.publishEvent(event)

        // then
        val allQuizzes = factQuizRepository.findAll()
        assertThat(allQuizzes).hasSize(initialQuizCount + 2)

        // 새로 생성된 퀴즈 검증
        val newQuizzes = allQuizzes.filter { it.realNews.id in realNewsIds }
        assertThat(newQuizzes).hasSize(2)

        newQuizzes.forEach { quiz ->
            assertThat(quiz.question).isNotBlank()
            assertThat(quiz.correctNewsType).isNotNull()
            assertThat(quiz.quizType).isEqualTo(QuizType.FACT)
            assertThat(quiz.fakeNews).isNotNull()
            assertThat(quiz.realNews).isNotNull()
            assertThat(quiz.fakeNews.id).isEqualTo(quiz.realNews.id)
        }

        // 개별 퀴즈 상세 검증
        verifySpecificQuiz(newQuizzes, managedRealNews1.id, "content1", "fakeContent1")
        verifySpecificQuiz(newQuizzes, managedRealNews2.id, "content2", "fakeContent2")
    }

    @Test
    @DisplayName("FakeNews가 없는 RealNews는 퀴즈 생성에서 제외됨")
    fun t2() {
        // given
        val realNewsWithFake = createRealNews("contentWithFake", "titleWithFake")
        val realNewsWithoutFake = createRealNews("contentWithoutFake", "titleWithoutFake")

        entityManager.persist(realNewsWithFake)
        entityManager.persist(realNewsWithoutFake)
        entityManager.flush()
        entityManager.clear()

        val managedRealNewsWithFake = realNewsRepository.findById(realNewsWithFake.id).orElseThrow()
        val managedRealNewsWithoutFake = realNewsRepository.findById(realNewsWithoutFake.id).orElseThrow()

        // 하나의 뉴스에만 FakeNews 생성
        val fakeNews = createFakeNews(managedRealNewsWithFake, "fakeContent")
        fakeNewsRepository.save(fakeNews)
        managedRealNewsWithFake.fakeNews = fakeNews

        val realNewsIds = listOf(
            managedRealNewsWithFake.id,
            managedRealNewsWithoutFake.id
        )

        // when
        val event = FakeNewsCreatedEvent(realNewsIds)
        eventPublisher.publishEvent(event)

        // then
        val allQuizzes = factQuizRepository.findAll()
        assertThat(allQuizzes).hasSize(initialQuizCount + 1) // FakeNews가 있는 것만 퀴즈 생성

        val newQuizzes = allQuizzes.filter { it.realNews.id in realNewsIds }
        assertThat(newQuizzes).hasSize(1)
        assertThat(newQuizzes[0].realNews.id).isEqualTo(managedRealNewsWithFake.id)
    }

    @Test
    @DisplayName("존재하지 않는 RealNews ID로 이벤트 발행시 퀴즈가 생성되지 않음")
    fun t3() {
        // given
        val nonExistentIds = listOf(999999L, 999998L)

        // when
        val event = FakeNewsCreatedEvent(nonExistentIds)
        eventPublisher.publishEvent(event)

        // then
        val allQuizzes = factQuizRepository.findAll()
        assertThat(allQuizzes).hasSize(initialQuizCount)
    }

    @Test
    @DisplayName("빈 ID 목록으로 이벤트 발행시 퀴즈가 생성되지 않음")
    fun t4() {
        // given
        val emptyIds = emptyList<Long>()

        // when
        val event = FakeNewsCreatedEvent(emptyIds)
        eventPublisher.publishEvent(event)

        // then
        val allQuizzes = factQuizRepository.findAll()
        assertThat(allQuizzes).hasSize(initialQuizCount) // 기존 개수와 동일
    }

    // Helper methods
    private fun createRealNews(content: String, title: String): RealNews {
        return RealNews(
            content,
            title,
            "description",
            "link",
            "imgUrl",
            LocalDateTime.now(),
            "mediaName",
            "journalist",
            "originalNewsUrl",
            LocalDateTime.now(),
            NewsCategory.IT,
            0
        )
    }

    private fun createFakeNews(realNews: RealNews, content: String) = FakeNews(realNews, content)

    private fun verifySpecificQuiz(
        quizzes: List<FactQuiz>,
        realNewsId: Long,
        expectedRealContent: String,
        expectedFakeContent: String
    ) {
        val quiz = quizzes.find { it.realNews.id == realNewsId }
            ?: throw AssertionError("ID $realNewsId 에 대한 퀴즈를 찾을 수 없습니다.")

        assertThat(quiz.realNews.content).isEqualTo(expectedRealContent)
        assertThat(quiz.fakeNews.content).isEqualTo(expectedFakeContent)
        assertThat(quiz.fakeNews.id).isEqualTo(quiz.realNews.id)
    }
}