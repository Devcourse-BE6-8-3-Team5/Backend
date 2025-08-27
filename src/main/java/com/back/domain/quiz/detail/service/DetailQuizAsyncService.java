package com.back.domain.quiz.detail.service;

import com.back.domain.quiz.detail.dto.DetailQuizDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetailQuizAsyncService {
    private final DetailQuizService detailQuizService;
    private final DetailQuizRateLimitedService detailQuizRateLimitedService;
    private final ConcurrentHashMap<Long, Boolean> inProgressMap = new ConcurrentHashMap<>();

    @Async("quizExecutor")
    public CompletableFuture<Void> generateAsync(long newsId) {
        if (inProgressMap.putIfAbsent(newsId, Boolean.TRUE) != null) {
            log.warn("이미 진행 중인 퀴즈 생성 작업이 있습니다. 뉴스 ID: " + newsId);
            return CompletableFuture.completedFuture(null); // 이미 진행 중인 작업이 있으면 바로 반환
        }

        CompletableFuture<Void> result;
        try {
            // Rate limit 적용하여 Ai 호출해 퀴즈 생성(트랜잭션 없음)
            List<DetailQuizDto> quizzes = detailQuizRateLimitedService.generatedQuizzesWithRateLimit(newsId);

            // 생성된 퀴즈를 DB에 저장(트랜잭션)
            detailQuizService.saveQuizzes(newsId, quizzes);

            log.info("상세 퀴즈 생성 완료, 뉴스 ID: " + newsId);
            result = CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("[실패] 뉴스 퀴즈 생성 실패 - newsId: {}, 오류: {}", newsId, e.getMessage(), e);
            result = CompletableFuture.completedFuture(null);

        } finally {
            inProgressMap.remove(newsId);
        }

        return result;
    }
}
