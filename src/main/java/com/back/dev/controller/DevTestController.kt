package com.back.dev.controller

import com.back.dev.service.DevTestNewsService
import com.back.domain.news.common.dto.KeywordGenerationResDto
import com.back.domain.news.common.dto.NaverNewsDto
import com.back.domain.news.common.service.KeywordGenerationService
import com.back.domain.news.real.dto.RealNewsDto
import com.back.domain.news.real.service.RealNewsService
import com.back.global.rsData.RsData
import com.back.global.rsData.RsData.Companion.of
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Profile("!prod")
@RestController
@RequestMapping("/ntest")
class DevTestController(
    private val devTestNewsService: DevTestNewsService,
    private val realNewsService: RealNewsService,
    private val keywordGenerationService: KeywordGenerationService
) {
    @GetMapping("/news")
    fun testCreateNews(): RsData<List<RealNewsDto>> {
        return runCatching {
            devTestNewsService.testNewsDataService()
        }.fold(
            onSuccess = { testNews -> of(200, "테스트 뉴스 생성 완료", testNews) },
            onFailure = { e -> of(500, "테스트 뉴스 생성 실패: ${e.message}", emptyList()) }
        )
    }

    @GetMapping("/fetch")
    fun testFetch(@RequestParam query: String): RsData<List<NaverNewsDto>> {
        val testNews = devTestNewsService.fetchNews(query)
        return of(200, "테스트 뉴스 메타데이터 생성 완료", testNews)
    }

    @GetMapping("/keyword")
    fun testKeyword(): RsData<KeywordGenerationResDto> {
        val keywords = keywordGenerationService.generateTodaysKeywords()

        return of(200, "테스트 키워드 생성 완료", keywords)
    }

    @GetMapping("/today")
    fun getTodayNews() : RsData<RealNewsDto?> {
        val todayNewsId = realNewsService.todayNewsOrRecent
        val todayNews = realNewsService.getRealNewsDtoById(todayNewsId)

        return todayNews?.let { news -> of(200, "오늘의 뉴스 조회 성공", news)}
            ?: of(404, "오늘의 뉴스를 찾을 수 없습니다.")
    }
}
