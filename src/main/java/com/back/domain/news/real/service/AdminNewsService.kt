package com.back.domain.news.real.service

import com.back.domain.news.common.dto.AnalyzedNewsDto
import com.back.domain.news.common.dto.NaverNewsDto
import com.back.domain.news.common.service.KeepAliveMonitoringService
import com.back.domain.news.common.service.KeywordGenerationService
import com.back.domain.news.real.dto.RealNewsDto
import com.back.domain.news.real.event.RealNewsCreatedEvent
import com.back.domain.news.today.service.TodayNewsService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class AdminNewsService(
    private val newsDataService: NewsDataService,
    private val keywordGenerationService: KeywordGenerationService,
    private val newsAnalysisService: NewsAnalysisService,
    private val publisher: ApplicationEventPublisher,
    private val keepAliveMonitoringService: KeepAliveMonitoringService,
    private val todayNewsService: TodayNewsService
) {

    companion object {
        private val log = LoggerFactory.getLogger(AdminNewsService::class.java)
        private val STATIC_KEYWORD = listOf("속보", "긴급", "단독")
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul") // 매일 자정에 실행
    @Transactional
    fun dailyNewsProcess() {
        keepAliveMonitoringService.startBatchKeepAlive()

        runCatching {
            val keywords: List<String> = keywordGenerationService.generateTodaysKeywords().keywords
            val newsKeywordsAfterAdd: MutableList<String> = newsDataService.addKeywords(keywords, STATIC_KEYWORD)
            val newsMetaData: List<NaverNewsDto> = newsDataService.collectMetaDataFromNaver(newsKeywordsAfterAdd)
            val newsAfterCrwal: List<RealNewsDto> = newsDataService.createRealNewsDtoByCrawl(newsMetaData)
            val newsAfterFilter: List<AnalyzedNewsDto> = newsAnalysisService.filterAndScoreNews(newsAfterCrwal)
            val selectedNews: List<RealNewsDto> = newsDataService.selectNewsByScore(newsAfterFilter)
            val savedNews: List<RealNewsDto> = newsDataService.saveAllRealNews(selectedNews)

            savedNews.firstOrNull()?.let { firstNews -> todayNewsService.setTodayNews(firstNews.id)
                publishNewsCreatedEvent(savedNews.map{ it.id })
                } ?: log.warn("저장된 뉴스가 없습니다. 오늘의 뉴스 수집이 실패했을 수 있습니다.")
        }.onFailure { e -> log.error("뉴스 처리 중 오류 발생",e) }
    }

    private fun publishNewsCreatedEvent(newsIds: List<Long>) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                publisher.publishEvent(RealNewsCreatedEvent(newsIds))
            }
        })
    }
}
