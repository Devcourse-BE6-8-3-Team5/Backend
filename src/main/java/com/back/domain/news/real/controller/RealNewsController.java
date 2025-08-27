package com.back.domain.news.real.controller;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.common.enums.NewsType;
import com.back.domain.news.common.service.NewsPageService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.NewsDataService;
import com.back.domain.news.real.service.RealNewsService;
import com.back.domain.news.today.entity.TodayNews;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Tag(name = "RealNewsController", description = "Real News API")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class RealNewsController {

    private final RealNewsService realNewsService;
    private final NewsPageService newsPageService;
    private final NewsDataService newsDataService;
    private static final int OX_QUIZ_INDEX = 1;

    //단건조회
    @Operation(summary = "단건 뉴스 조회", description = "ID로 단건 뉴스를 조회합니다.")

    @GetMapping("/{newsId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "뉴스 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 뉴스 ID"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 뉴스를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public RsData<RealNewsDto> getRealNewsById(@PathVariable Long newsId) {

        if (newsId == null || newsId <= 0) {
            return RsData.of(400, "잘못된 뉴스 ID입니다. 1 이상의 숫자를 입력해주세요.");
        }

        Long todayNewsId = realNewsService.getTodayNewsOrRecent();

        if (todayNewsId!= -1L && newsId.equals(todayNewsId)) {
            return RsData.of(403, "오늘의 뉴스는 탭을 통해 조회해주세요.");
        }

        Optional<RealNewsDto> realNewsDto = realNewsService.getRealNewsDtoById(newsId);

        if (realNewsDto.isEmpty()) {
            return RsData.of(404,
                    String.format("ID %d에 해당하는 뉴스를 찾을 수 없습니다. 올바른 뉴스 ID인지 확인해주세요.", newsId));
        }

        return newsPageService.getSingleNews(realNewsDto, NewsType.REAL, newsId);
    }

    @Operation(summary = "오늘의 뉴스 조회", description = "선정된 오늘의 뉴스를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "조회할 뉴스가 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/today")
    public RsData<RealNewsDto> getTodayNews() {
        Long todayNewsId = realNewsService.getTodayNewsOrRecent();
        Optional<RealNewsDto> todayNews = realNewsService.getRealNewsDtoById(todayNewsId);

        if (todayNews.isEmpty()) {
            return RsData.of(404, "조회할 뉴스가 없습니다.");
        }

        return newsPageService.getSingleNews(todayNews, NewsType.REAL, todayNews.get().id());
    }

    //다건조회(시간순)
    @Operation(summary = "다건 뉴스 조회", description = "페이지네이션을 통해 시간순으로 다건 뉴스를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "뉴스 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public RsData<Page<RealNewsDto>> getRealNewsList(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (1~100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 방향 (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String direction
    ) {
        if (!isValidPageParam(page, size, direction)) {
            return RsData.of(400, "잘못된 페이지 파라미터입니다");
        }

        Pageable pageable = PageRequest.of(page-1, size);

        Page<RealNewsDto> realNewsPage = realNewsService.getRealNewsListExcludingNth(pageable, OX_QUIZ_INDEX);

        return newsPageService.getPagedNews(realNewsPage, NewsType.REAL);
    }

    //다건조회(검색)
    @Operation(summary = "뉴스 검색", description = "제목으로 뉴스를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "뉴스 검색 성공"),
            @ApiResponse(responseCode = "400", description = "검색어가 비어있거나 잘못된 페이지 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/search")
    public RsData<Page<RealNewsDto>> searchRealNewsByTitle(
            @Parameter(description = "검색할 뉴스 제목", example = "경제")
            @RequestParam String title,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (1~100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 방향 (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String direction
    ) {
        if (title == null || title.trim().isEmpty()) {
            return RsData.of(400, "검색어를 입력해주세요");
        }

        if (!isValidPageParam(page, size, direction)) {
            return RsData.of(400, "잘못된 페이지 파라미터입니다");
        }

        Pageable pageable = PageRequest.of(page-1, size);
        Page<RealNewsDto> RealNewsPage = realNewsService.searchRealNewsByTitleExcludingNth(title,pageable, OX_QUIZ_INDEX);

        return newsPageService.getPagedNews(RealNewsPage, NewsType.REAL);
    }

    @Operation(summary = "카테고리별 뉴스 조회", description = "카테고리별로 뉴스를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카테고리별 뉴스 조회 성공"),
            @ApiResponse(responseCode = "400", description = "올바르지 않은 카테고리이거나 잘못된 페이지 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/category/{category}")
    public RsData<Page<RealNewsDto>> getRealNewsByCategory(
            @Parameter(description = "뉴스 카테고리", example = "ECONOMY",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            allowableValues = {"POLITICS", "ECONOMY", "IT", "CULTURE", "SOCIETY"}))
            @PathVariable String category,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (1~100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 방향 (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String direction
    ) {
        if (!isValidPageParam(page, size, direction)) {
            return RsData.of(400, "잘못된 페이지 파라미터입니다");
        }

        NewsCategory newsCategory;

        try {
            newsCategory = NewsCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RsData.of(400, "올바르지 않은 카테고리입니다. 사용 가능한 카테고리: " +
                    Arrays.toString(NewsCategory.values()));
        }

        Pageable pageable = PageRequest.of(page-1, size);
        Page<RealNewsDto> realNewsPage = realNewsService.getRealNewsListByCategoryExcludingNth(newsCategory, pageable, OX_QUIZ_INDEX);

        return newsPageService.getPagedNews(realNewsPage, NewsType.REAL);
    }

    private boolean isValidPageParam(int page, int size, String direction) {
        return (direction.equals("asc") || direction.equals("desc"))
                && page > 0
                && size >= 1 && size <= 100;
    }

}
