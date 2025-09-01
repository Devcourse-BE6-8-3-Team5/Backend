package com.back.domain.news.real.service

import com.back.domain.news.common.dto.NaverNewsDto
import com.back.global.exception.ServiceException
import com.back.global.rateLimiter.RateLimiter
import com.back.global.util.HtmlEntityDecoder
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.concurrent.CompletableFuture

@Service
class NaverNewsService(
    private val newsDeduplicationService: NewsDeduplicationService,
    private val restTemplate: RestTemplate,
    private val rateLimiter: RateLimiter,
    private val objectMapper: ObjectMapper,
    @Value("\${NAVER_CLIENT_ID}") private val clientId: String,
    @Value("\${NAVER_CLIENT_SECRET}") private val clientSecret: String,
    @Value("\${naver.news.display}") private val newsDisplayCount: Int,
    @Value("\${naver.crawling.delay}") private val crawlingDelay: Int,
    @Value("\${naver.base-url}") private val naverUrl: String,
    @Value("\${naver.news.sort}") private val newsSortOrder: String,
    @Value("\${news.dedup.description.threshold}") private var  descriptionSimilarityThreshold: Double,
    @Value("\${news.dedup.title.threshold}") private var  titleSimilarityThreshold: Double
) {
    companion object {
        private val log = LoggerFactory.getLogger(NaverNewsService::class.java)
    }

    // 서비스 초기화 시 설정값 검증
    @PostConstruct
    fun validateConfig() {
        require(clientId.isNotBlank()) { "NAVER_CLIENT_ID가 설정되지 않았습니다." }
        require( clientSecret.isNotBlank()) { "NAVER_CLIENT_SECRET가 설정되지 않았습니다." }
        require(newsDisplayCount in 1..99) { "NAVER_NEWS_DISPLAY_COUNT는 100이하의 값이어야 합니다." }
        require(crawlingDelay >= 0) { "NAVER_CRAWLING_DELAY는 0 이상이어야 합니다." }
        require(naverUrl.isNotBlank()) { "NAVER_BASE_URL이 설정되지 않았습니다." }
        require(newsSortOrder.isNotBlank()) {"NAER_NEWS_SORT가 제대로 설정되지않았습니다"}
    }

    @Async("newsExecutor")
    fun fetchNews(keyword: String): CompletableFuture<List<NaverNewsDto>> {
        return runCatching {
            rateLimiter.waitForRateLimit()

            val url = "$naverUrl$keyword&display=$newsDisplayCount&sort=$newsSortOrder"

            val headers = HttpHeaders().apply {
                set("X-Naver-Client-Id", clientId)
                set("X-Naver-Client-Secret", clientSecret)
            }

            val entity = HttpEntity<String>(headers)
            val response = restTemplate.exchange(url, HttpMethod.GET, entity, String::class.java)

            if (response.statusCode != HttpStatus.OK) {
                throw ServiceException(500, "네이버 API 호출 실패: ${response.statusCode}")
            }

            val items = objectMapper.readTree(response.body)?.get("items")
                ?: return@runCatching emptyList<NaverNewsDto>()

            val rawNews = getNewsMetaDataFromNaverApi(items)
            val naverOnly = rawNews.filter { it.link.contains("n.news.naver.com") }

            val dedupTitle = newsDeduplicationService.removeDuplicateByBitSetByField(naverOnly, { it.title }, titleSimilarityThreshold)
            val dedupDescription = newsDeduplicationService.removeDuplicateByBitSetByField(dedupTitle, { it.description }, descriptionSimilarityThreshold)
            val limited = dedupDescription.take(12)

            log.info("키워드 '${keyword}': 원본 ${naverOnly.size}개 → 중복제거 후 ${dedupDescription.size}개 → 제한 후 ${limited.size}개")

            limited
        }.fold(
            onSuccess = { CompletableFuture.completedFuture(it) },
            onFailure = { e ->
                when (e) {
                    is JsonProcessingException -> throw ServiceException(500, "네이버 API 응답 파싱 실패")
                    is ServiceException -> throw e
                    else -> throw RuntimeException("네이버 뉴스 조회 중 오류 발생", e)
                }
            }
        )
    }

    // fetchNews 메서드로 네이버 API에서 뉴스 목록을 가져오고
    // 링크 정보를 바탕으로 상세 정보를 crawlAddtionalInfo 메서드로 크롤링하여 RealNews 객체를 생성
    private fun getNewsMetaDataFromNaverApi(items: JsonNode): List<NaverNewsDto> {

        return items.mapNotNull { item ->
            val rawTitle = item.get("title")?.asText("") ?: return@mapNotNull null
            val originallink = item.get("originallink")?.asText("") ?: return@mapNotNull null
            val link = item.get("link")?.asText("") ?: return@mapNotNull null
            val rawDescription = item.get("description")?.asText("") ?: return@mapNotNull null
            val pubDate = item.get("pubDate")?.asText("") ?: return@mapNotNull null

            val cleanedTitle = HtmlEntityDecoder.decode(rawTitle)
            val cleanDescription = HtmlEntityDecoder.decode(rawDescription)

            // 모든 필드가 비어있지 않은 경우만 DTO 생성
            if (listOf(cleanedTitle, originallink, link, cleanDescription, pubDate).all { it.isNotBlank() }) {
                NaverNewsDto(cleanedTitle, originallink, link, cleanDescription, pubDate)
            } else null
        }
    }
}