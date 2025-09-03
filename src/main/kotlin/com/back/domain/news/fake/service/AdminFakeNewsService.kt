package com.back.domain.news.fake.service

import com.back.domain.news.common.service.KeepAliveMonitoringService
import com.back.domain.news.fake.dto.FakeNewsDto
import com.back.domain.news.fake.event.FakeNewsCreatedEvent
import com.back.domain.news.real.dto.RealNewsDto
import com.back.domain.news.real.service.RealNewsService
import com.back.standard.extensions.executeWithKeepAlive
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service


@Service
class AdminFakeNewsService(
    private val fakeNewsService: FakeNewsService,
    private val realNewsService: RealNewsService,
    private val publisher: ApplicationEventPublisher,
    private val keepAliveMonitoringService: KeepAliveMonitoringService,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(AdminFakeNewsService::class.java)
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    fun dailyFakeNewsProcess() {
        keepAliveMonitoringService.executeWithKeepAlive {
            processRealNewsToFakeNews()
        }
    }

    private fun processRealNewsToFakeNews() {
        val realNewsDtos = realNewsService.getRealNewsListCreatedToday()

        if (realNewsDtos.isEmpty()) {
            log.warn("오늘 생성된 실제 뉴스가 없습니다.")
            return
        }

        log.info("처리 대상 실제 뉴스: {}개", realNewsDtos.size)

        runCatching {
            fakeNewsService.generateAndSaveAllFakeNews(realNewsDtos)
        }.fold(
            onSuccess = { fakeNewsDtos ->
                handleProcessSuccess(realNewsDtos, fakeNewsDtos)
            },
            onFailure = { e ->
                log.error("가짜 뉴스 생성 중 오류 발생", e)
                throw e
            }
        )
    }

    private fun handleProcessSuccess(realNewsDtos: List<RealNewsDto>, fakeNewsDtos: List<FakeNewsDto>) {
        // 이벤트 발행
        val successIds = fakeNewsDtos.map { it.realNewsId }
        publisher.publishEvent(FakeNewsCreatedEvent(successIds))

        // 결과 로깅
        val stats = BatchProcessStats(
            requested = realNewsDtos.size,
            succeeded = fakeNewsDtos.size,
            failed = realNewsDtos.size - fakeNewsDtos.size
        )

        log.info("=== 일일 가짜뉴스 생성 배치 완료 ===")
        log.info("요청: {}개, 성공: {}개, 실패: {}개", stats.requested, stats.succeeded, stats.failed)
    }

    private data class BatchProcessStats(
        val requested: Int,
        val succeeded: Int,
        val failed: Int
    )
}
