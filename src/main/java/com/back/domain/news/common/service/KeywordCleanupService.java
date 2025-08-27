package com.back.domain.news.common.service;

import com.back.domain.news.common.repository.KeywordHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordCleanupService {
    private final KeywordHistoryRepository keywordHistoryRepository;

    @Value("${keyword.cleanup.retention-days}")
    private int retentionDays;

    @Transactional
    public void cleanupKeywords(){
        // 현재 날짜에서 retentionDays 만큼 이전 날짜 계산(7이면 7일 이전 데이터 삭제)
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);

        try{
            int deletedCount = keywordHistoryRepository.deleteByUsedDateBefore(cutoffDate);
            log.info("키워드 정리 완료 - {}일 이전 키워드 {}개 삭제", retentionDays, deletedCount);
        } catch(Exception e){
            log.error("키워드 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 관리자 키워드 수동 삭제
    @Transactional
    public int adminCleanup(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);

        try{
            int deletedCount = keywordHistoryRepository.deleteByUsedDateBefore(cutoffDate);
            log.info("키워드 정리 완료 - {}일 이전 키워드 {}개 삭제", days, deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("키워드 정리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
