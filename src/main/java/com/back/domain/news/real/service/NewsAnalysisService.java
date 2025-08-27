package com.back.domain.news.real.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAnalysisService {

    private final NewsAnalysisBatchService newsAnalysisBatchService;

    @Value("${news.filter.batch.size:2}") // 배치 크기 설정, 3으로 하면 본문 길면 깨짐
    private int batchSize;


    public List<AnalyzedNewsDto> filterAndScoreNews(List<RealNewsDto> allRealNewsBeforeFilter) {
        if (allRealNewsBeforeFilter == null || allRealNewsBeforeFilter.isEmpty()) {
            log.warn("필터링할 뉴스가 없습니다.");
            return List.of();
        }

        log.info("뉴스 필터링 시작 - 총 {}개", allRealNewsBeforeFilter.size());

        List<List<RealNewsDto>> batches= new ArrayList<>();

        // batch_size로 나누어서 처리
        for (int i = 0; i < allRealNewsBeforeFilter.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, allRealNewsBeforeFilter.size());
            batches.add(allRealNewsBeforeFilter.subList(i, endIndex));
        }

        List<AnalyzedNewsDto> allResults = Collections.synchronizedList(new ArrayList<>());

        // 비동기 작업들 시작
        List<CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> newsAnalysisBatchService.processBatchAsync(batch)
                        .thenAccept(result -> {
                            // 완료되는 대로 즉시 결과에 추가
                            allResults.addAll(result);
                            log.info("배치 완료 - 현재까지 {}개 처리됨", allResults.size());
                        })
                        .exceptionally(throwable -> {
                            log.error("배치 처리 실패", throwable);

                            // TaskRejectedException 특별 처리
                            if (isTaskRejectionException(throwable)) {
                                log.error("스레드풀 용량 부족 - 스레드 크기 확인 필요");
                            }
                            return null;
                        }))
                .toList();

        try {
            // 모든 작업 완료 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                log.error("뉴스 분석 작업이 인터럽트됨", cause);
            }
            log.error("뉴스 분석 중 일부 오류 발생했지만 계속 진행", cause);
        }

        log.info("뉴스 필터링 완료 - 최종 결과: {}개", allResults.size());
        return new ArrayList<>(allResults);
    }

    // TaskRejectedException 감지
    private boolean isTaskRejectionException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TaskRejectedException ||
                    current.getClass().getSimpleName().contains("TaskReject")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
