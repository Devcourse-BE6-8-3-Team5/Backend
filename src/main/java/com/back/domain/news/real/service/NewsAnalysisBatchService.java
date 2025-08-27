package com.back.domain.news.real.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.NewsAnalysisProcessor;
import com.back.global.rateLimiter.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAnalysisBatchService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    @Async("newsExecutor")
    public CompletableFuture<List<AnalyzedNewsDto>> processBatchAsync(List<RealNewsDto> batch) {
        log.info("스레드: {}, 배치 시작", Thread.currentThread().getName());

        try {
            rateLimiter.waitForRateLimit();

            NewsAnalysisProcessor processor = new NewsAnalysisProcessor(batch, objectMapper);
            List<AnalyzedNewsDto> result = aiService.process(processor);

            log.info("스레드: {}, 배치 완료 - {}개", Thread.currentThread().getName(), result.size());
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("배치 처리 실패", e);
            return CompletableFuture.completedFuture(List.of());
        }
    }
}