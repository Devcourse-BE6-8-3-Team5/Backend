package com.back.domain.quiz.daily.eventListener

import com.back.domain.news.today.event.TodayNewsCreatedEvent
import com.back.domain.news.today.repository.TodayNewsRepository
import com.back.domain.news.today.service.TodayNewsService
import com.back.domain.quiz.daily.repository.DailyQuizRepository
import com.back.domain.quiz.detail.event.DetailQuizCreatedEvent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.awaitility.Awaitility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.annotation.Commit
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "HEALTHCHECK_URL=health_check_url"
    ]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DailyQuizEventListenerTest {
    @Autowired
    private lateinit var todayNewsService: TodayNewsService

    @Autowired
    private lateinit var dailyQuizRepository: DailyQuizRepository

    @Autowired
    private lateinit var todayNewsRepository: TodayNewsRepository

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private var initialQuizCount = 0

    @BeforeEach
    fun setup() {
        dailyQuizRepository.deleteAll()
        initialQuizCount = dailyQuizRepository.count().toInt()
    }

    @Test
    @DisplayName("DetailQuizCreatedEvent 발생 시 오늘의 퀴즈가 정상 생성되는지 검증")
    fun t1() {
        // Given
        val todayNewsId = todayNewsRepository.findAll()[0].id

        // When
        transactionTemplate.execute {
            eventPublisher.publishEvent(DetailQuizCreatedEvent())
            null
        }

        // Then - 비동기 처리 완료 대기
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val dailyQuizzes = dailyQuizRepository.findByTodayNewsId(todayNewsId)
            assertThat(dailyQuizzes).hasSize(3)
        }

        dailyQuizRepository.findByTodayNewsId(todayNewsId).forEach { dailyQuiz ->
            assertThat(dailyQuiz.todayNews.id).isEqualTo(todayNewsId)
            assertThat(dailyQuiz.todayNews.selectedDate).isEqualTo(LocalDate.now())
            assertThat(dailyQuiz.detailQuiz).isNotNull
            assertThat(dailyQuiz.detailQuiz.realNews.id).isEqualTo(todayNewsId)
        }
    }

    @Test
    @DisplayName("TodayNewsCreatedEvent 발생 시 오늘의 퀴즈가 정상 생성되는지 검증")
    @Commit
    fun t2() {
        // Given
        val todayNewsId = 5L

        // When
        todayNewsService.setTodayNews(todayNewsId) // 내부에서 TodayNewsCreatedEvent 발행

        // Then - 비동기 처리 완료 대기
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val dailyQuizzes = dailyQuizRepository.findByTodayNewsId(todayNewsId)
            assertThat(dailyQuizzes).hasSize(3)
        }

        dailyQuizRepository.findByTodayNewsId(todayNewsId).forEach { dailyQuiz ->
            assertThat(dailyQuiz.todayNews).isNotNull
            assertThat(dailyQuiz.todayNews.id).isEqualTo(todayNewsId)
            assertThat(dailyQuiz.detailQuiz).isNotNull
            assertThat(dailyQuiz.detailQuiz.realNews.id).isEqualTo(todayNewsId)
        }
    }

    @Test
    @DisplayName("이미 생성된 오늘의 퀴즈에 대해 중복 생성을 방지하는지 검증")
    fun t3() {
        // Given
        val todayNewsId = 1L

        // When - 동일한 이벤트를 다시 발행
        eventPublisher.publishEvent(TodayNewsCreatedEvent(todayNewsId))
        waitForAsyncCompletion()

        eventPublisher.publishEvent(TodayNewsCreatedEvent(todayNewsId))
        waitForAsyncCompletion()

        // Then - 퀴즈 개수가 증가하지 않았는지 확인
        val finalQuizCount = dailyQuizRepository.count()
        assertThat(finalQuizCount).isEqualTo(initialQuizCount.toLong())
    }

    @Test
    @DisplayName("존재하지 않는 TodayNews ID로 이벤트 발생 시 예외 처리 검증")
    fun t4() {
        // Given
        val nonExistentTodayNewsId = 999L

        // When & Then - 예외가 발생해도 시스템이 중단되지 않는지 확인
        assertThatCode {
            eventPublisher.publishEvent(TodayNewsCreatedEvent(nonExistentTodayNewsId))
            waitForAsyncCompletion()
        }.doesNotThrowAnyException()

        // 퀴즈가 생성되지 않았는지 확인
        val finalQuizCount = dailyQuizRepository.count()
        assertThat(finalQuizCount).isEqualTo(initialQuizCount.toLong())
    }

    @Test
    @DisplayName("연결된 상세 퀴즈가 없는 뉴스에 대해 예외 처리 검증")
    fun t5() {
        // Given - 상세 퀴즈가 없는 뉴스 ID (뉴스 8번)
        val todayNewsIdWithoutQuizzes = 8L

        // When & Then - 예외가 발생해도 시스템이 중단되지 않는지 확인
        assertThatCode {
            eventPublisher.publishEvent(TodayNewsCreatedEvent(todayNewsIdWithoutQuizzes))
            waitForAsyncCompletion()
        }.doesNotThrowAnyException()

        // 퀴즈가 생성되지 않았는지 확인
        val finalQuizCount = dailyQuizRepository.count()
        assertThat(finalQuizCount).isEqualTo(initialQuizCount.toLong())
    }

    @Test
    @DisplayName("특정 DetailQuiz가 이미 DailyQuiz로 생성된 경우 중복 생성 방지 검증")
    @Commit
    fun t6() {
        // Given
        val todayNewsId = 1L

        // 첫 번째 생성
        eventPublisher.publishEvent(TodayNewsCreatedEvent(todayNewsId))
        waitForAsyncCompletion()

        val initialQuizzes = dailyQuizRepository.findAll()
        val initialCount = initialQuizzes.size

        // When - 같은 DetailQuiz에 대해 다시 생성 시도
        // (실제로는 서비스 로직에서 중복 체크를 통해 건너뜀)
        eventPublisher.publishEvent(TodayNewsCreatedEvent(todayNewsId))
        waitForAsyncCompletion()

        // Then
        val finalQuizzes = dailyQuizRepository.findAll()
        assertThat(finalQuizzes).hasSize(initialCount)

        // 동일한 DetailQuiz ID들이 중복 생성되지 않았는지 확인
        val detailQuizIds = finalQuizzes.map { it.detailQuiz.id }
        assertThat(detailQuizIds).doesNotHaveDuplicates()
    }

    // 헬퍼 메서드
    private fun waitForAsyncCompletion() {
        try {
            // 비동기 작업 완료를 위한 충분한 대기 시간
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Interrupted while waiting for async completion", e)
        }
    }
}
