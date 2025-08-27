package com.back.domain.quiz.daily.eventListener;

import com.back.domain.news.today.event.TodayNewsCreatedEvent;
import com.back.domain.quiz.daily.service.DailyQuizService;
import com.back.domain.quiz.detail.event.DetailQuizCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyQuizEventListener {
    private final DailyQuizService dailyQuizService;

    @Async("dailyQuizExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTodayNewsCreated(TodayNewsCreatedEvent event) {
        Long todayNewsId = event.getTodayNewsId();
        try {
            // 오늘의 뉴스 ID를 사용하여 오늘의 퀴즈 생성
            dailyQuizService.createDailyQuiz(todayNewsId);
        } catch (Exception e) {
            log.error("오늘의 퀴즈 생성 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @Async("dailyQuizExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDetailQuizCreated(DetailQuizCreatedEvent event) {
        try {
            // dailyNewsProcess() 메서드로 생성된 오늘의 뉴스에 대한 퀴즈를 생성(상세 퀴즈 생성 후)
            dailyQuizService.createDailyQuiz();
        } catch (Exception e) {
            log.error("오늘의 퀴즈 생성 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
