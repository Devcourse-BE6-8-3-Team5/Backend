package com.back.domain.news.real.controller

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.common.enums.NewsType
import com.back.domain.news.common.service.NewsPageService
import com.back.domain.news.real.dto.RealNewsDto
import com.back.domain.news.real.service.RealNewsService
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@Tag(name = "RealNewsController", description = "Real News API")
@RestController
@RequestMapping("/api/news")
class RealNewsController(
    private val realNewsService: RealNewsService,
    private val newsPageService: NewsPageService,
    @Value("\${news.exclude.index}") private val excludeIndex: Int
) {

    companion object {
        private val VALID_DIRECTIONS = setOf("asc", "desc")
        private const val MIN_PAGE = 1
        private const val MIN_SIZE = 1
        private const val MAX_SIZE = 100
    }

    @GetMapping("/{newsId}")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "뉴스 조회 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 뉴스 ID"),
        ApiResponse(responseCode = "404", description = "해당 ID의 뉴스를 찾을 수 없음"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    ])
    @Operation(summary = "단건 뉴스 조회", description = "ID로 단건 뉴스를 조회합니다.")
    fun getRealNewsById(@PathVariable newsId: Long): RsData<RealNewsDto?> {
        if (newsId <= 0) {
            return RsData.of(400, "잘못된 뉴스 ID입니다. 1 이상의 숫자를 입력해주세요.")
        }

        val todayNewsId = realNewsService.todayNewsOrRecent
        if (todayNewsId != -1L && newsId == todayNewsId) {
            return RsData.of(403, "오늘의 뉴스는 탭을 통해 조회해주세요.")
        }

        val realNewsDto = realNewsService.getRealNewsDtoById(newsId)
        return newsPageService.getSingleNews(realNewsDto, NewsType.REAL, newsId)
    }

    @GetMapping("/today")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "404", description = "조회할 뉴스가 없음"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    ])
    @Operation(summary = "오늘의 뉴스 조회", description = "선정된 오늘의 뉴스를 조회합니다.")
    fun getTodayNews(): RsData<RealNewsDto?> {
        val todayNewsId = realNewsService.todayNewsOrRecent
        val todayNews = realNewsService.getRealNewsDtoById(todayNewsId)
        return newsPageService.getSingleNews(todayNews, NewsType.REAL, todayNewsId)
    }

    @GetMapping
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "뉴스 목록 조회 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 페이지 파라미터"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    ])
    @Operation(summary = "다건 뉴스 조회", description = "페이지네이션을 통해 시간순으로 다건 뉴스를 조회합니다.")
    fun getRealNewsList(
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "페이지 크기 (1~100)", example = "10")
        @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "정렬 방향 (asc/desc)", example = "desc")
        @RequestParam(defaultValue = "desc") direction: String
    ): RsData<Page<RealNewsDto>?> {
        validatePageParams(page, size, direction)?.let { return it }

        val pageable = PageRequest.of(page - 1, size)
        val realNewsPage = realNewsService.getRealNewsListExcludingNth(pageable, excludeIndex)
        return newsPageService.getPagedNews(realNewsPage, NewsType.REAL)
    }

    @GetMapping("/search")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "뉴스 검색 성공"),
        ApiResponse(responseCode = "400", description = "검색어가 비어있거나 잘못된 페이지 파라미터"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    ])
    @Operation(summary = "뉴스 검색", description = "제목으로 뉴스를 검색합니다.")
    fun searchRealNewsByTitle(
        @Parameter(description = "검색할 뉴스 제목", example = "경제")
        @RequestParam title: String?,
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "페이지 크기 (1~100)", example = "10")
        @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "정렬 방향 (asc/desc)", example = "desc")
        @RequestParam(defaultValue = "desc") direction: String
    ): RsData<Page<RealNewsDto>?> {
        if (title.isNullOrBlank()) {
            return RsData.of(400, "검색어를 입력해주세요")
        }

        validatePageParams(page, size, direction)?.let { return it }

        val pageable = PageRequest.of(page - 1, size)
        val realNewsPage = realNewsService.searchRealNewsByTitleExcludingNth(title, pageable, excludeIndex)
        return newsPageService.getPagedNews(realNewsPage, NewsType.REAL)
    }

    @GetMapping("/category/{category}")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "카테고리별 뉴스 조회 성공"),
        ApiResponse(responseCode = "400", description = "올바르지 않은 카테고리이거나 잘못된 페이지 파라미터"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    ])
    @Operation(summary = "카테고리별 뉴스 조회", description = "카테고리별로 뉴스를 조회합니다")
    fun getRealNewsByCategory(
        @Parameter(
            description = "뉴스 카테고리",
            example = "ECONOMY",
            schema = Schema(allowableValues = ["POLITICS", "ECONOMY", "IT", "CULTURE", "SOCIETY"])
        ) @PathVariable category: String,
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "페이지 크기 (1~100)", example = "10")
        @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "정렬 방향 (asc/desc)", example = "desc")
        @RequestParam(defaultValue = "desc") direction: String
    ): RsData<Page<RealNewsDto>?> {
        validatePageParams(page, size, direction)?.let { return it }

        val newsCategory = NewsCategory.entries
            .find { it.name.equals(category, ignoreCase = true) }
            ?: return RsData.of(
                400,
                "올바르지 않은 카테고리입니다. 사용 가능한 카테고리: ${NewsCategory.entries.joinToString()}"
            )

        val pageable = PageRequest.of(page - 1, size)
        val realNewsPage = realNewsService.getRealNewsListByCategoryExcludingNth(newsCategory, pageable, excludeIndex)
        return newsPageService.getPagedNews(realNewsPage, NewsType.REAL)
    }

    private fun validatePageParams(page: Int, size: Int, direction: String): RsData<Page<RealNewsDto>?>? {
        return when {
            direction !in VALID_DIRECTIONS -> RsData.of(400, "정렬 방향은 'asc' 또는 'desc'여야 합니다")
            page < MIN_PAGE -> RsData.of(400, "페이지 번호는 1 이상이어야 합니다")
            size !in MIN_SIZE..MAX_SIZE -> RsData.of(400, "페이지 크기는 $MIN_SIZE~$MAX_SIZE 사이여야 합니다")
            else -> null
        }
    }
}