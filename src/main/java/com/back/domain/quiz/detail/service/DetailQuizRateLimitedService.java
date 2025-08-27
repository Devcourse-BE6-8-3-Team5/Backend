package com.back.domain.quiz.detail.service;

import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.global.exception.ServiceException;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetailQuizRateLimitedService {
    private final DetailQuizService detailQuizService;
    private final Bucket bucket;

    public List<DetailQuizDto> generatedQuizzesWithRateLimit(Long newsId) throws InterruptedException {
        int maxRetries = 5; // 최대 재시도 횟수
        int retryDelay = 60000; // 재시도 대기 시간 (밀리초 단위)

        for(int i=0; i<maxRetries; i++){
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit 제한으로 재시도 대기중... 시도 횟수: {} - newsId: {} ", i+1, newsId);
                Thread.sleep(retryDelay);
                continue;
            }
            try {
                // Rate limit이 허용되면 AI 호출해 퀴즈 생성
                return detailQuizService.generateQuizzes(newsId);
            } catch (Exception e) {
                log.warn("AI 호출 중 오류 발생. 재시도합니다. 시도 횟수: {} - newsId: {}, error: {}", i+1, newsId, e.getMessage());
                Thread.sleep(retryDelay);
            }
        }
        log.error("퀴즈 생성 최종 실패. 뉴스 ID: {}", newsId);
        throw new ServiceException(500, "퀴즈 생성 최종 실패. 뉴스 ID: " + newsId);

    }
}
