package com.back.domain.news.fake.service

import com.back.domain.news.fake.dto.FakeNewsDto
import com.back.domain.news.fake.entity.FakeNews
import com.back.domain.news.fake.repository.FakeNewsRepository
import com.back.domain.news.real.dto.RealNewsDto
import com.back.domain.news.real.repository.RealNewsRepository
import com.back.global.ai.AiService
import com.back.global.ai.processor.FakeNewsGeneratorProcessor
import com.back.global.rateLimiter.RateLimiter
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.Executor

@Service
class FakeNewsService(
    private val aiService: AiService,
    private val objectMapper: ObjectMapper,
    private val fakeNewsRepository: FakeNewsRepository,
    private val realNewsRepository: RealNewsRepository,
    private val rateLimiter: RateLimiter,
    @Qualifier("newsExecutor") private val executor: Executor
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(FakeNewsService::class.java)
    }

    fun generateFakeNewsBatch(realNewsDtos: List<RealNewsDto>): CompletableFuture<List<FakeNewsDto>> {
        if (realNewsDtos.isEmpty()) {
            log.warn("생성할 가짜뉴스가 없습니다.")
            return completedFuture(emptyList())
        }

        log.info("가짜뉴스 배치 생성 시작 (비동기) - 총 {}개", realNewsDtos.size)

        val futures = realNewsDtos.map { realNewsDto ->
            supplyAsync({
                runCatching {
                    rateLimiter.waitForRateLimit()
                    log.debug("가짜뉴스 생성 시작 - 실제뉴스 ID: {}", realNewsDto.id)

                    val processor = FakeNewsGeneratorProcessor(realNewsDto, objectMapper)
                    val result = aiService.process(processor)

                    log.debug("가짜뉴스 생성 완료 - 실제뉴스 ID: {}", realNewsDto.id)
                    result
                }.fold(
                    onSuccess = { it },
                    onFailure = { e ->
                        log.error("가짜뉴스 생성 실패 - 실제뉴스 ID: {}", realNewsDto.id, e)
                        null
                    }
                )
            }, executor) // executor 사용
        }

        // null 아닌 성공 결과 수집
        val results = futures.mapNotNull { it.join() }

        return completedFuture(results)
    }

    @Transactional
    fun generateAndSaveAllFakeNews(realNewsDtos: List<RealNewsDto>): List<FakeNewsDto> {
        return runCatching {
            val fakeNewsDtos = generateFakeNewsBatch(realNewsDtos).get()

            if (fakeNewsDtos.isEmpty()) {
                log.warn("생성된 가짜뉴스가 없습니다.")
                return emptyList()
            }

            saveFakeNewsForBatch(fakeNewsDtos)
            fakeNewsDtos
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                log.error("가짜 뉴스 생성 및 저장 실패: {}", e.message)
                emptyList()
            }
        )
    }

    @Transactional
    fun saveAllFakeNews(fakeNewsDtos: List<FakeNewsDto>) {
        val realNewsIds: List<Long> = fakeNewsDtos.map{ it.realNewsId }.toList()

        val realNewsMap = realNewsRepository.findAllById(realNewsIds).associateBy{ it.id }

        val fakeNewsList = fakeNewsDtos
            .mapNotNull{ dto ->
                realNewsMap[dto.realNewsId]?.let { realNews ->
                    FakeNews(
                        realNews = realNews,
                        content = dto.content
                    )
                }
            }

        fakeNewsRepository.saveAll(fakeNewsList)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveFakeNewsForBatch(fakeNewsDtos: List<FakeNewsDto>) {
        log.info("=== FakeNews 배치 저장 시작 - 입력: ${fakeNewsDtos.size}개 ===")
        if (fakeNewsDtos.isEmpty()) {
            log.warn("저장할 FakeNewsDto가 없습니다.")
            return
        }

        val uniqueDtos = fakeNewsDtos.distinctBy { it.realNewsId }
        val existingIds = fakeNewsRepository.findExistingIds(uniqueDtos.map { it.realNewsId }).toSet()
        val newDtos = uniqueDtos.filterNot { it.realNewsId in existingIds }

        log.debug("처리 현황 - 신규: {}개, 기존: {}개", newDtos.size, uniqueDtos.size - newDtos.size)

        newDtos.takeIf { it.isNotEmpty() }

            ?.runCatching {
                map { dto ->
                    FakeNews(
                        realNews = realNewsRepository.getReferenceById(dto.realNewsId),
                        content = dto.content
                    )
                }.let { fakeNewsRepository.saveAll(it) }
            }?.fold(
                onSuccess = { saved ->
                    log.info("=== 배치 저장 완료 - 성공: {}개, 스킵: {}개 ===",
                        saved.count(), uniqueDtos.size - newDtos.size)
                },
                onFailure = { e ->
                    log.error("배치 저장 실패", e)
                    throw e
                }
            ) ?: log.info("저장할 신규 FakeNews가 없습니다.")
    }

    fun count(): Int {
        return fakeNewsRepository.count().toInt()

    }
}

