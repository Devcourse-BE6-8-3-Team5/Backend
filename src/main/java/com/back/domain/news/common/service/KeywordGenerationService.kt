package com.back.domain.news.common.service

import com.back.domain.news.common.dto.KeywordGenerationReqDto
import com.back.domain.news.common.dto.KeywordGenerationResDto
import com.back.domain.news.common.dto.KeywordWithType
import com.back.domain.news.common.enums.KeywordType
import com.back.global.ai.AiService
import com.back.global.ai.processor.KeywordGeneratorProcessor
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate


@Service
class KeywordGenerationService(
    private val aiService: AiService,
    private val objectMapper: ObjectMapper,
    private val keywordHistoryService: KeywordHistoryService,
    private val keywordCleanupService: KeywordCleanupService,
    @Value("\${keyword.overuse.days}") private val overuseDays: Int,
    @Value("\${keyword.overuse.threshold}")private val overuseThreshold : Int,
    @Value("\${keyword.history.recent-days}")private val recentDays: Int
) {
    companion object{
        private val log = LoggerFactory.getLogger(KeywordGenerationService::class.java)
    }

    @PostConstruct
    fun validateConfig() {
        require(overuseDays in 1..30) { "KEYWORD_OVERUSE_DAYS는 1에서 30 사이여야 합니다." }
        require(overuseThreshold in 1..30) { "KEYWORD_OVERUSE_THRESHOLD는 1에서 30 사이여야 합니다." }
        require(recentDays in 1..30) { "KEYWORD_OVERUSE_RECENT-DAYS는  1에서 30 사이여야 합니다." }

    }


    fun generateTodaysKeywords(): KeywordGenerationResDto {
        val today = LocalDate.now()
        val excludeKeywords = getExcludeKeywords()
        val recentKeywords = keywordHistoryService.getRecentKeywords(recentDays)

        runCatching { keywordCleanupService.cleanupKeywords() }
            .onFailure { e -> log.warn("키워드 정리 중 오류 발생: ${e.message}") }

        val keywordGenerationReqDto = KeywordGenerationReqDto(today, recentKeywords, excludeKeywords)
        log.info("키워드 생성 요청 - 날짜 :  $today , 제외 키워드 : $excludeKeywords")

        return runCatching {
            val processor = KeywordGeneratorProcessor(keywordGenerationReqDto, objectMapper)
            aiService.process(processor)
                .also { result ->
                    log.info("키워드 생성 결과 - $result")
                    keywordHistoryService.saveKeywords(result, today)
                }
        }.getOrElse { e ->
            log.error("키워드 생성 실패, 기본 키워드 사용: ${e.message}")
            createDefaultCase().also { defaultCaseResult ->
                keywordHistoryService.saveKeywords(defaultCaseResult, today)
            }
        }
    }

    private fun createDefaultCase(): KeywordGenerationResDto {
        return KeywordGenerationResDto(
            society = listOf("사회", "교육").toKeywords(),
            economy = listOf("경제", "시장").toKeywords(),
            politics = listOf("정치", "정부").toKeywords(),
            culture = listOf("문화", "예술").toKeywords(),
            it = listOf("기술", "IT").toKeywords()
        )
    }

    private fun List<String>.toKeywords(): List<KeywordWithType> =
        map { KeywordWithType(it, KeywordType.GENERAL) }

    fun getExcludeKeywords(): List<String> {

        return buildList {
            // 과도하게 사용된 키워드 추가
            addAll(keywordHistoryService.getOverusedKeywords(overuseDays, overuseThreshold))
            // 어제 사용된 키워드 추가
            addAll(keywordHistoryService.getYesterdayKeywords())
        }.distinct().also { excludeKeywords ->
            log.debug("제외 키워드 목록: {}", excludeKeywords)
        }
    }
}
