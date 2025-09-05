package com.back.domain.news.real.service

import com.back.domain.news.common.service.KeepAliveMonitoringService
import com.back.domain.news.common.service.KeywordGenerationService
import com.back.domain.news.real.dto.RealNewsDto
import com.back.domain.news.real.event.RealNewsCreatedEvent
import com.back.domain.news.today.service.TodayNewsService
import com.back.standard.extensions.executeWithKeepAlive
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
        private val STATIC_KEYWORDS = listOf("속보", "긴급", "단독")
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    fun dailyNewsProcess() {
        keepAliveMonitoringService.executeWithKeepAlive {
            executeNewsProcessingPipeline()
        }
    }

    private fun executeNewsProcessingPipeline() {
        newsProcessingPipeline()
            .fold(
                onSuccess = { savedNews -> handleProcessingSuccess(savedNews) },
                onFailure = { e -> log.error("뉴스 처리 중 오류 발생", e) }
            )
    }

    private fun newsProcessingPipeline(): Result<List<RealNewsDto>> = runCatching {
        keywordGenerationService.generateTodaysKeywords().keywords
            .let { generated -> newsDataService.addKeywords(generated, STATIC_KEYWORDS) }
            .let { keywords -> newsDataService.collectMetaDataFromNaver(keywords) }
            .let { metadata -> newsDataService.createRealNewsByWebCrawling(metadata) }
            .let { crawledNews -> newsAnalysisService.filterAndScoreNews(crawledNews) }
            .let { analyzedNews -> newsDataService.selectNewsByScore(analyzedNews) }
            .let { selectedNews -> newsDataService.saveAllRealNews(selectedNews) }
    }

    private fun handleProcessingSuccess(savedNews: List<RealNewsDto>) {
        savedNews
            .takeIf { it.isNotEmpty() }
            ?.also { news -> todayNewsService.setTodayNews(news.first().id) }
            ?.map { it.id }
            ?.let { ids -> publishNewsCreatedEvent(ids) }
            ?: log.warn("저장된 뉴스가 없습니다. 오늘의 뉴스 수집이 실패했을 수 있습니다.")
    }

    private fun publishNewsCreatedEvent(newsIds: List<Long>) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                publisher.publishEvent(RealNewsCreatedEvent(newsIds))
            }
        })
    }
}
