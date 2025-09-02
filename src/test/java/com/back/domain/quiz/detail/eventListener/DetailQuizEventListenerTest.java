package com.back.domain.quiz.detail.eventListener;

import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.event.RealNewsCreatedEvent;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.detail.repository.DetailQuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "HEALTHCHECK_URL=health_check_url",
        "GEMINI_API_KEY=gemini_api_key"
})
public class DetailQuizEventListenerTest {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RealNewsRepository realNewsRepository;

    @Autowired
    private DetailQuizRepository detailQuizRepository;

    private int initialQuizCount;

    @BeforeEach
    void setUp() {
        initialQuizCount = (int) detailQuizRepository.count();
    }

    @Test
    @DisplayName("RealNewsCreatedEvent가 발행되면 상세 퀴즈가 생성되어 DB에 저장됨")
    @Disabled("실제 AI 호출 테스트 - 필요할 때만 실행")
    void t1(){
        // Given
        List<Long> newsIds = realNewsRepository.findAll().stream().map(RealNews::getId).toList();
        RealNewsCreatedEvent event = new RealNewsCreatedEvent(newsIds);
        int newsCount = newsIds.size();

        // When
        eventPublisher.publishEvent(event);
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            List<DetailQuiz> quizzes = detailQuizRepository.findAll();
            assertThat(quizzes).hasSize(newsCount*3);
        });

        // Then
        List<DetailQuiz> quizzes = detailQuizRepository.findAll();
        quizzes.forEach(quiz -> {
            assertThat(quiz.getQuestion()).isNotBlank();
            assertThat(quiz.getOption1()).isNotBlank();
            assertThat(quiz.getOption2()).isNotBlank();
            assertThat(quiz.getOption3()).isNotBlank();
            assertThat(quiz.getCorrectOption()).isNotNull();
            assertThat(quiz.getQuizType()).isEqualTo(QuizType.DETAIL);
            assertThat(quiz.getRealNews()).isNotNull();
        });

    }

    @Test
    @DisplayName("존재하지 않는 RealNews ID로 이벤트 발행시 퀴즈가 생성되지 않음")
    void t2(){
        // Given
        List<Long> newsIds = List.of(999L);
        RealNewsCreatedEvent event = new RealNewsCreatedEvent(newsIds);

        // When
        eventPublisher.publishEvent(event);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Then
        List<DetailQuiz> quizzes = detailQuizRepository.findAll();

        assertThat(quizzes).hasSize(initialQuizCount);
    }

    private void waitForAsyncCompletion() {
        try {
            // 비동기 작업 완료를 위한 충분한 대기 시간
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for async completion", e);
        }
    }

}
