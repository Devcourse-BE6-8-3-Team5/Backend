package com.back.backend.domain.quiz.daily.eventListener;

import com.back.domain.news.real.service.NewsDataService;
import com.back.domain.news.today.event.TodayNewsCreatedEvent;
import com.back.domain.news.today.repository.TodayNewsRepository;
import com.back.domain.quiz.daily.entity.DailyQuiz;
import com.back.domain.quiz.daily.repository.DailyQuizRepository;
import com.back.domain.quiz.detail.event.DetailQuizCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "HEALTHCHECK_URL=health_check_url",
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DailyQuizEventListenerTest {
    @Autowired
    private NewsDataService newsDataService;
    @Autowired
    private DailyQuizRepository dailyQuizRepository;
    @Autowired
    private TodayNewsRepository todayNewsRepository;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setupAndClearData() {
        dailyQuizRepository.deleteAll();
    }

    @Test
    @DisplayName("DetailQuizCreatedEvent 발생 시 오늘의 퀴즈가 정상 생성되는지 검증")
    void t1() {
        // Given
        long todayNewsId = todayNewsRepository.findAll().get(0).getId();

        // When
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new DetailQuizCreatedEvent());
            return null; // execute 메서드는 리턴값 필요
        });

        // Then - 비동기 처리 완료 대기
        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<DailyQuiz> dailyQuizzes = dailyQuizRepository.findByTodayNewsId(todayNewsId);
            assertThat(dailyQuizzes).isNotEmpty();
            assertThat(dailyQuizzes).hasSize(3);
        });

        List<DailyQuiz> result = dailyQuizRepository.findByTodayNewsId(todayNewsId);
        result.forEach(dailyQuiz -> {
            assertThat(dailyQuiz.getTodayNews().getId()).isEqualTo(todayNewsId);
            assertThat(dailyQuiz.getTodayNews().getSelectedDate()).isEqualTo(LocalDate.now());
            assertThat(dailyQuiz.getDetailQuiz()).isNotNull();
            assertThat(dailyQuiz.getDetailQuiz().getRealNews().getId()).isEqualTo(todayNewsId);
        });
    }

    @Test
    @DisplayName("TodayNewsCreatedEvent 발생 시 오늘의 퀴즈가 정상 생성되는지 검증")
    @Commit
    void t2() {
        // Given
        Long todayNewsId = 5L;

        // When
        newsDataService.setTodayNews(todayNewsId); // 내부에서 TodayNewsCreatedEvent 발행

        // Then - 비동기 처리 완료 대기
        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<DailyQuiz> dailyQuizzes = dailyQuizRepository.findByTodayNewsId(todayNewsId);
            assertThat(dailyQuizzes).isNotEmpty();
            assertThat(dailyQuizzes).hasSize(3);
        });

        List<DailyQuiz> result = dailyQuizRepository.findByTodayNewsId(todayNewsId);
        result.forEach(dailyQuiz -> {
            assertThat(dailyQuiz.getTodayNews()).isNotNull();
            assertThat(dailyQuiz.getTodayNews().getId()).isEqualTo(todayNewsId);
            assertThat(dailyQuiz.getDetailQuiz()).isNotNull();
            assertThat(dailyQuiz.getDetailQuiz().getRealNews().getId()).isEqualTo(todayNewsId);
        });
    }

    @Test
    @DisplayName("이미 생성된 오늘의 퀴즈에 대해 중복 생성을 방지하는지 검증")
    void t3() {
        // Given
        Long todayNewsId = 1L;

        // 첫 번째 퀴즈 생성
        eventPublisher.publishEvent(new TodayNewsCreatedEvent(todayNewsId));
        waitForAsyncCompletion();

        long initialQuizCount = dailyQuizRepository.count();

        // When - 동일한 이벤트를 다시 발행
        eventPublisher.publishEvent(new TodayNewsCreatedEvent(todayNewsId));
        waitForAsyncCompletion();

        // Then - 퀴즈 개수가 증가하지 않았는지 확인
        long finalQuizCount = dailyQuizRepository.count();
        assertThat(finalQuizCount).isEqualTo(initialQuizCount);
    }

    @Test
    @DisplayName("존재하지 않는 TodayNews ID로 이벤트 발생 시 예외 처리 검증")
    void t4() {
        // Given
        Long nonExistentTodayNewsId = 999L;

        // When & Then - 예외가 발생해도 시스템이 중단되지 않는지 확인
        assertThatCode(() -> {
            eventPublisher.publishEvent(new TodayNewsCreatedEvent(nonExistentTodayNewsId));
            waitForAsyncCompletion();
        }).doesNotThrowAnyException();

        // 퀴즈가 생성되지 않았는지 확인
        List<DailyQuiz> dailyQuizzes = dailyQuizRepository.findAll();
        assertThat(dailyQuizzes).isEmpty();
    }

    @Test
    @DisplayName("연결된 상세 퀴즈가 없는 뉴스에 대해 예외 처리 검증")
    void t5() {
        // Given - 상세 퀴즈가 없는 뉴스 ID (뉴스 8번)
        Long todayNewsIdWithoutQuizzes = 8L;

        // When & Then - 예외가 발생해도 시스템이 중단되지 않는지 확인
        assertThatCode(() -> {
            eventPublisher.publishEvent(new TodayNewsCreatedEvent(todayNewsIdWithoutQuizzes));
            waitForAsyncCompletion();
        }).doesNotThrowAnyException();

        // 퀴즈가 생성되지 않았는지 확인
        List<DailyQuiz> dailyQuizzes = dailyQuizRepository.findAll();
        assertThat(dailyQuizzes).isEmpty();
    }

    @Test
    @DisplayName("특정 DetailQuiz가 이미 DailyQuiz로 생성된 경우 중복 생성 방지 검증")
    @Commit
    void t6() {
        // Given
        Long todayNewsId = 1L;

        // 첫 번째 생성
        eventPublisher.publishEvent(new TodayNewsCreatedEvent(todayNewsId));
        waitForAsyncCompletion();

        List<DailyQuiz> initialQuizzes = dailyQuizRepository.findAll();
        int initialCount = initialQuizzes.size();

        // When - 같은 DetailQuiz에 대해 다시 생성 시도
        // (실제로는 서비스 로직에서 중복 체크를 통해 건너뜀)
        eventPublisher.publishEvent(new TodayNewsCreatedEvent(todayNewsId));
        waitForAsyncCompletion();

        // Then
        List<DailyQuiz> finalQuizzes = dailyQuizRepository.findAll();
        assertThat(finalQuizzes).hasSize(initialCount);

        // 동일한 DetailQuiz ID들이 중복 생성되지 않았는지 확인
        List<Long> detailQuizIds = finalQuizzes.stream()
                .map(quiz -> quiz.getDetailQuiz().getId())
                .toList();

        assertThat(detailQuizIds).doesNotHaveDuplicates();
    }

    // 헬퍼 메서드
    private void waitForAsyncCompletion() {
        try {
            // 비동기 작업 완료를 위한 충분한 대기 시간
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for async completion", e);
        }
    }
}
