package com.back.domain.quiz.detail.eventListener

import com.back.domain.news.real.event.RealNewsCreatedEvent
import com.back.domain.news.real.repository.RealNewsRepository
import com.back.domain.quiz.QuizType
import com.back.domain.quiz.detail.repository.DetailQuizRepository
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@TestPropertySource(
    properties = ["NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "HEALTHCHECK_URL=health_check_url",
        "GEMINI_API_KEY=gemini_api_key"
    ]
)
class DetailQuizEventListenerTest {
    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var realNewsRepository: RealNewsRepository

    @Autowired
    private lateinit var detailQuizRepository: DetailQuizRepository

    private var initialQuizCount = 0

    @BeforeEach
    fun setUp() {
        initialQuizCount = detailQuizRepository.count().toInt()
    }

    @Test
    @DisplayName("RealNewsCreatedEvent가 발행되면 상세 퀴즈가 생성되어 DB에 저장됨")
    @Disabled("실제 AI 호출 테스트 - 필요할 때만 실행")
    fun t1() {
        // Given
        val newsIds = realNewsRepository.findAll().map { it.id }
        val event = RealNewsCreatedEvent(newsIds)
        val newsCount = newsIds.size

        // When
        eventPublisher.publishEvent(event)
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            val quizzes = detailQuizRepository.findAll()
            assertThat(quizzes).hasSize(newsCount * 3)
        }

        // Then
        val quizzes = detailQuizRepository.findAll()
        quizzes.forEach { quiz ->
            assertThat(quiz.question).isNotBlank()
            assertThat(quiz.option1).isNotBlank()
            assertThat(quiz.option2).isNotBlank()
            assertThat(quiz.option3).isNotBlank()
            assertThat(quiz.correctOption).isNotNull()
            assertThat(quiz.quizType).isEqualTo(QuizType.DETAIL)
            assertThat(quiz.realNews).isNotNull()
        }
    }

    @Test
    @DisplayName("존재하지 않는 RealNews ID로 이벤트 발행시 퀴즈가 생성되지 않음")
    fun t2() {
        // Given
        val newsIds = listOf(999L)
        val event = RealNewsCreatedEvent(newsIds)

        // When
        eventPublisher.publishEvent(event)

        // Then
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)   // 최대 5초 기다림
            .untilAsserted {
                val quizzes = detailQuizRepository.findAll()
                assertThat(quizzes).hasSize(initialQuizCount)
            }
    }
}
