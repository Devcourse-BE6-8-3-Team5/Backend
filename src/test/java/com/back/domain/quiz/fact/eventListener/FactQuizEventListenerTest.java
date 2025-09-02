package com.back.domain.quiz.fact.eventListener;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.fake.entity.FakeNews;
import com.back.domain.news.fake.event.FakeNewsCreatedEvent;
import com.back.domain.news.fake.repository.FakeNewsRepository;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.fact.entity.FactQuiz;
import com.back.domain.quiz.fact.repository.FactQuizRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "HEALTHCHECK_URL=health_check_url"
})
public class FactQuizEventListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RealNewsRepository realNewsRepository;

    @Autowired
    private FakeNewsRepository fakeNewsRepository;

    @Autowired
    private FactQuizRepository factQuizRepository;

    @Autowired
    private EntityManager entityManager;

    private int initialQuizCount;

    @BeforeEach
    void setUp() {
        initialQuizCount = (int) factQuizRepository.count();
    }

    @Test
    @DisplayName("FakeNewsCreatedEvent가 발행되면 팩트 퀴즈가 생성되어 DB에 저장됨")
    void t1() {
        // given
        RealNews realNews1 = createRealNews("content1", "title1");
        RealNews realNews2 = createRealNews("content2", "title2");

        entityManager.persist(realNews1);
        entityManager.persist(realNews2);
        entityManager.flush();
        entityManager.clear();

        RealNews managedRealNews1 = realNewsRepository.findById(realNews1.getId()).orElseThrow();
        RealNews managedRealNews2 = realNewsRepository.findById(realNews2.getId()).orElseThrow();

        FakeNews fakeNews1 = createFakeNews(managedRealNews1, "fakeContent1");
        FakeNews fakeNews2 = createFakeNews(managedRealNews2, "fakeContent2");

        fakeNewsRepository.saveAll(List.of(fakeNews1, fakeNews2));

        managedRealNews1.setFakeNews(fakeNews1);
        managedRealNews2.setFakeNews(fakeNews2);

        List<Long> realNewsIds = List.of(managedRealNews1.getId(), managedRealNews2.getId());

        // when
        FakeNewsCreatedEvent event = new FakeNewsCreatedEvent(realNewsIds);
        eventPublisher.publishEvent(event);

        // then
        List<FactQuiz> allQuizzes = factQuizRepository.findAll();
        assertThat(allQuizzes).hasSize(initialQuizCount + 2);

        // 새로 생성된 퀴즈 검증
        List<FactQuiz> newQuizzes = allQuizzes.stream()
                .filter(q -> realNewsIds.contains(q.getRealNews().getId()))
                .toList();

        assertThat(newQuizzes).hasSize(2);

        newQuizzes.forEach(quiz -> {
            assertThat(quiz.getQuestion()).isNotBlank();
            assertThat(quiz.getCorrectNewsType()).isNotNull();
            assertThat(quiz.getQuizType()).isEqualTo(QuizType.FACT);
            assertThat(quiz.getFakeNews()).isNotNull();
            assertThat(quiz.getRealNews()).isNotNull();
            assertThat(quiz.getFakeNews().getId()).isEqualTo(quiz.getRealNews().getId());
        });

        // 개별 퀴즈 상세 검증
        verifySpecificQuiz(newQuizzes, managedRealNews1.getId(), "content1", "fakeContent1");
        verifySpecificQuiz(newQuizzes, managedRealNews2.getId(), "content2", "fakeContent2");
    }

    @Test
    @DisplayName("FakeNews가 없는 RealNews는 퀴즈 생성에서 제외됨")
    void t2() {
        // given
        RealNews realNewsWithFake = createRealNews("contentWithFake", "titleWithFake");
        RealNews realNewsWithoutFake = createRealNews("contentWithoutFake", "titleWithoutFake");

        entityManager.persist(realNewsWithFake);
        entityManager.persist(realNewsWithoutFake);
        entityManager.flush();
        entityManager.clear();

        RealNews managedRealNewsWithFake = realNewsRepository.findById(realNewsWithFake.getId()).orElseThrow();
        RealNews managedRealNewsWithoutFake = realNewsRepository.findById(realNewsWithoutFake.getId()).orElseThrow();

        // 하나의 뉴스에만 FakeNews 생성
        FakeNews fakeNews = createFakeNews(managedRealNewsWithFake, "fakeContent");
        fakeNewsRepository.save(fakeNews);
        managedRealNewsWithFake.setFakeNews(fakeNews);

        List<Long> realNewsIds = List.of(
                managedRealNewsWithFake.getId(),
                managedRealNewsWithoutFake.getId()
        );

        // when
        FakeNewsCreatedEvent event = new FakeNewsCreatedEvent(realNewsIds);
        eventPublisher.publishEvent(event);

        // then
        List<FactQuiz> allQuizzes = factQuizRepository.findAll();
        assertThat(allQuizzes).hasSize(initialQuizCount + 1); // FakeNews가 있는 것만 퀴즈 생성

        List<FactQuiz> newQuizzes = allQuizzes.stream()
                .filter(q -> realNewsIds.contains(q.getRealNews().getId()))
                .toList();

        assertThat(newQuizzes).hasSize(1);
        assertThat(newQuizzes.get(0).getRealNews().getId()).isEqualTo(managedRealNewsWithFake.getId());
    }

    @Test
    @DisplayName("존재하지 않는 RealNews ID로 이벤트 발행시 퀴즈가 생성되지 않음")
    void t3() {
        // given
        List<Long> nonExistentIds = List.of(999999L, 999998L);

        // when
        FakeNewsCreatedEvent event = new FakeNewsCreatedEvent(nonExistentIds);
        eventPublisher.publishEvent(event);

        // then
        List<FactQuiz> allQuizzes = factQuizRepository.findAll();
        assertThat(allQuizzes).hasSize(initialQuizCount);
    }

    @Test
    @DisplayName("빈 ID 목록으로 이벤트 발행시 퀴즈가 생성되지 않음")
    void t4() {
        // given
        List<Long> emptyIds = Collections.emptyList();

        // when
        FakeNewsCreatedEvent event = new FakeNewsCreatedEvent(emptyIds);
        eventPublisher.publishEvent(event);

        // then
        List<FactQuiz> allQuizzes = factQuizRepository.findAll();
        assertThat(allQuizzes).hasSize(initialQuizCount); // 기존 개수와 동일
    }

    // Helper methods
    private RealNews createRealNews(String content, String title) {
        return new RealNews(
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
        );
    }

    private FakeNews createFakeNews(RealNews realNews, String content) {
        return new FakeNews(
                    realNews,
                    content
                );
    }

    private void verifySpecificQuiz(List<FactQuiz> quizzes, Long realNewsId, String expectedRealContent, String expectedFakeContent) {
        FactQuiz quiz = quizzes.stream()
                .filter(q -> q.getRealNews().getId() == realNewsId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("ID " + realNewsId + "에 대한 퀴즈를 찾을 수 없습니다."));

        assertThat(quiz.getRealNews().getContent()).isEqualTo(expectedRealContent);
        assertThat(quiz.getFakeNews().getContent()).isEqualTo(expectedFakeContent);
        assertThat(quiz.getFakeNews().getId()).isEqualTo(quiz.getRealNews().getId());
    }
}