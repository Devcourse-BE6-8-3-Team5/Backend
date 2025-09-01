package com.back.domain.news.common.service

import com.back.domain.news.common.repository.KeywordHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class KeywordCleanupService(
    private val keywordHistoryRepository: KeywordHistoryRepository
) {
    @Value("\${keyword.cleanup.retention-days}")
    private val retentionDays = 0

    companion object {
        val log = LoggerFactory.getLogger(KeywordCleanupService::class.java)
    }

    @Transactional
    fun cleanupKeywords() {
        // 현재 날짜에서 retentionDays 만큼 이전 날짜 계산(7이면 7일 이전 데이터 삭제)
        val cutoffDate = LocalDate.now().minusDays(retentionDays.toLong())

        runCatching {
            val deletedCount = keywordHistoryRepository.deleteByUsedDateBefore(cutoffDate)
            log.info("키워드 정리 완료 - {}일 이전 키워드 {}개 삭제", retentionDays, deletedCount)
        }.getOrElse { e ->
            log.error("키워드 정리 중 오류 발생: ${e.message}")
            throw e
        }
    }

    // 관리자 키워드 수동 삭제
    @Transactional
    fun adminCleanup(days: Int) {
        val cutoffDate = LocalDate.now().minusDays(days.toLong())

        runCatching {
            keywordHistoryRepository.deleteByUsedDateBefore(cutoffDate)
        }.onSuccess { deletedCount ->
            log.info("관리자 키워드 정리 완료 - {}일 이전 키워드 {}개 삭제", retentionDays, deletedCount)
        }.onFailure { e ->
            log.error("관리자 키워드 정리 중 오류 발생: ${e.message}")
            throw e
        }
    }
}
