package com.back.domain.news.real.service

import com.back.domain.news.common.dto.AnalyzedNewsDto
import com.back.domain.news.common.dto.NaverNewsDto
import com.back.domain.news.common.dto.NewsDetailDto
import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.dto.RealNewsDto
import com.back.domain.news.real.entity.RealNews
import com.back.domain.news.real.mapper.RealNewsMapper
import com.back.domain.news.real.repository.RealNewsRepository
import com.back.domain.news.today.repository.TodayNewsRepository
import com.back.global.util.HtmlEntityDecoder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.stream.Stream

@Service
class NewsDataService(
    private val naverNewsService: NaverNewsService,
    private val newsCrawlingService: NewsCrawlingService,
    private val realNewsRepository: RealNewsRepository,
    private val todayNewsRepository: TodayNewsRepository,
    private val realNewsMapper: RealNewsMapper,
    @Value("\${naver.crawling.delay}") private val crawlingDelay: Int
) {

    companion object {
        private val log = LoggerFactory.getLogger(NewsDataService::class.java)
    }

    @Transactional
    fun createRealNewsByWebCrawling(metaDataList: List<NaverNewsDto>): List<RealNewsDto> {
        val allRealNewsDtos = mutableListOf<RealNewsDto>()

        return runCatching {
            for (metaData in metaDataList) {
                val newsDetailData = newsCrawlingService.crawlAdditionalInfo(metaData.link)

                if (newsDetailData == null) {
                    log.warn("크롤링 실패: {}", metaData.link)
                    continue
                }

                val realNewsDto = makeRealNewsFromInfo(metaData, newsDetailData)
                log.info("새 뉴스 생성 - 제목: {}", realNewsDto.title)
                allRealNewsDtos.add(realNewsDto)

                // 크롤링 간격 조절 (서버 부하 방지)
                Thread.sleep(crawlingDelay.toLong())
            }
            allRealNewsDtos.toList()
        }.onFailure { e ->
            when (e) {
                is InterruptedException -> {
                    Thread.currentThread().interrupt() // 인터럽트 상태 복원
                    log.error("크롤링 중 인터럽트 발생", e)
                }
                else -> log.error("크롤링 중 예외 발생", e)
            }
        }.getOrElse { emptyList() }
    }

    @Transactional
    fun saveAllRealNews(realNewsDtoList: List<RealNewsDto>): List<RealNewsDto> {
        // DTO → Entity 변환 후 저장
        val realNewsList: List<RealNews> = realNewsMapper.toEntityList(realNewsDtoList)
        val savedEntities = realNewsRepository.saveAll(realNewsList) // 저장된 결과 받기

        return realNewsMapper.toDtoList(savedEntities)
    }

    // 네이버 API를 통해 메타데이터 수집
    fun collectMetaDataFromNaver(keywords: List<String>): List<NaverNewsDto> {
        log.info("네이버 API 호출 시작: {} 개 키워드", keywords.size)
        val futures = keywords.map(naverNewsService::fetchNews)

        return runCatching {
            CompletableFuture.allOf(*futures.toTypedArray()).get()
            futures.flatMap { future ->
                future.get().filter { dto -> dto.link.contains("n.news.naver.com") }
            }
        }.onFailure { e ->
            when (e) {
                is InterruptedException -> log.error("뉴스 조회가 인터럽트됨", e)
                is ExecutionException -> log.error("뉴스 조회 중 오류 발생", e.cause)
                else -> log.error("예상치 못한 오류 발생", e)
            }
        }.getOrElse { emptyList() }
    }


    // 네이버 api에서 받아온 정보와 크롤링한 상세 정보를 바탕으로 RealNewsDto 생성
    fun makeRealNewsFromInfo(naverNewsDto: NaverNewsDto, newsDetailDto: NewsDetailDto): RealNewsDto {
        return RealNewsDto(
            0L,
            naverNewsDto.title ?: "",
            newsDetailDto.content,
            naverNewsDto.description ?: "",
            naverNewsDto.link,
            newsDetailDto.imgUrl,
            parseNaverDate(naverNewsDto.pubDate),
            LocalDateTime.now(),  // 생성일은 현재 시간으로 설정
            newsDetailDto.mediaName,
            newsDetailDto.journalist,
            naverNewsDto.originallink,
            NewsCategory.NOT_FILTERED
        )
    }


    // 네이버 API에서 받아온 날짜 문자열을 LocalDateTime으로 변환
    private fun parseNaverDate(naverDate: String): LocalDateTime {
        return runCatching {
            val cleaned = HtmlEntityDecoder.decode(naverDate)
            val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

            ZonedDateTime.parse(cleaned, formatter).toLocalDateTime()
        }.onFailure {
            log.warn("날짜 파싱 실패: {}. 현재 시간으로 설정", naverDate)
        }.getOrElse { LocalDateTime.now() }

    }

    @Transactional
    fun deleteRealNews(newsId: Long): Boolean {
        return realNewsRepository.findById(newsId).orElse(null)?.let { realNews ->
            if (todayNewsRepository.existsById(newsId))
                todayNewsRepository.deleteById(newsId)

            realNewsRepository.deleteById(newsId)
            true
        } ?: false
    }


    fun count(): Int = realNewsRepository.count().toInt()

    fun selectNewsByScore(allRealNewsAfterFilter: List<AnalyzedNewsDto>): List<RealNewsDto> {
        return allRealNewsAfterFilter
            .groupBy { it.category }
            .values
            .flatMap { categoryNews -> categoryNews.sortedByDescending { it.score }.take(4) }
            .map { it.realNewsDto}
    }

    fun addKeywords(keywords: List<String>, staticKeyword: List<String>): MutableList<String> {
        return Stream.concat(keywords.stream(), staticKeyword.stream())
            .distinct()
            .toList()
    }



}
