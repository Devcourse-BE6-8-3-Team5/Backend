package com.back.dev.service

import com.back.domain.news.common.dto.NaverNewsDto
import com.back.domain.news.fake.service.FakeNewsService
import com.back.domain.news.real.dto.RealNewsDto
import com.back.domain.news.real.service.NewsAnalysisService
import com.back.domain.news.real.service.NewsDataService
import com.back.global.standard.util.Ut.json.objectMapper
import com.back.global.util.HtmlEntityDecoder
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Profile("!prod")
@Service
class DevTestNewsService(
    private val newsDataService: NewsDataService,
    private val restTemplate: RestTemplate,
    private val newsAnalysisService: NewsAnalysisService,
    private val fakeNewsService: FakeNewsService,
    @Value("\${NAVER_CLIENT_ID}") private val clientId: String,
    @Value("\${NAVER_CLIENT_SECRET}") private val clientSecret: String,
    @Value("\${naver.news.display}") private val newsDisplayCount: Int,
    @Value("\${naver.news.sort:sim}") private val newsSortOrder: String,
    @Value("\${naver.base-url}") private val naverUrl: String
) {

    fun testNewsDataService(): List<RealNewsDto> {
        val newsKeywordsAfterAdd = listOf("AI")
        val newsAfterRemoveDup = newsDataService.collectMetaDataFromNaver(newsKeywordsAfterAdd)
        val newsAfterCrwal = newsDataService.createRealNewsDtoByCrawl(newsAfterRemoveDup)
        val newsAfterFilter = newsAnalysisService.filterAndScoreNews(newsAfterCrwal)
        val selectedNews = newsDataService.selectNewsByScore(newsAfterFilter)
        val savedNews = newsDataService.saveAllRealNews(selectedNews)
        val fakeNews = fakeNewsService.generateAndSaveAllFakeNews(savedNews)

        return savedNews
    }

    fun fetchNews(query: String): List<NaverNewsDto> = runCatching {
        // URL 구성 - 문자열 템플릿 사용
        val url = "$naverUrl$query&display=$newsDisplayCount&sort=$newsSortOrder"

        val headers = HttpHeaders().apply {
            set("X-Naver-Client-Id", clientId)
            set("X-Naver-Client-Secret", clientSecret)
        }

        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<String?>(headers),
            String::class.java
        )

        when (response.statusCode) {
            HttpStatus.OK -> {
                response.body?.let { responseBody ->
                    objectMapper.readTree(responseBody)
                        .get("items")
                        ?.let(::getNewsMetaDataFromNaverApi)
                        ?: emptyList()
                } ?: emptyList()
            }
            else -> throw RuntimeException("네이버 API 요청 실패: ${response.statusCode}")
        }
    }.fold(
        onSuccess = { it },
        onFailure = { exception ->
            when (exception) {
                is JsonProcessingException -> throw RuntimeException("JSON 파싱 실패", exception)
                else -> throw exception
            }
        }
    )

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

            if (listOf(cleanedTitle, originallink, link, cleanDescription, pubDate).all { it?.isNotBlank() ?: true }) {
                NaverNewsDto(cleanedTitle, originallink, link, cleanDescription, pubDate)
            } else null
        }
    }
}
