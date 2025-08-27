package com.back.domain.news.real.controller;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.common.enums.NewsType;
import com.back.domain.news.common.service.NewsPageService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.AdminNewsService;
import com.back.domain.news.real.service.NewsDataService;
import com.back.domain.news.real.service.RealNewsService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.hibernate.dialect.OracleXmlJdbcType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.domain.Sort.Direction.fromString;

@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "AdminNewsController", description = "관리자용 뉴스 생성, 조회, 삭제 API")
public class AdminNewsController {
    private final AdminNewsService adminNewsService;
    private final NewsDataService newsDataService;
    private final RealNewsService realNewsService;
    private final NewsPageService newsPageService;

    // 오늘의 뉴스 설정용 뉴스 조회
    @GetMapping("/all")
    @Operation(summary = "전체 뉴스 조회 (관리자용)", description = "오늘의 뉴스를 포함한 모든 뉴스를 조회합니다")
    public RsData<Page<RealNewsDto>> getAllRealNewsList(
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

        Sort.Direction sortDirection = fromString(direction);
        Sort sortBy = Sort.by(sortDirection, "originCreatedDate");

        Pageable pageable = PageRequest.of(page-1, size, sortBy);
        Page<RealNewsDto> realNewsPage = newsDataService.getAllRealNewsList(pageable);  // 새 서비스 메서드

        return newsPageService.getPagedNews(realNewsPage, NewsType.REAL);
    }

//     뉴스 배치 프로세서
    @GetMapping("/process")
    public RsData<List<RealNewsDto>> newsProcess() {
        try {
            adminNewsService.dailyNewsProcess();

            return RsData.of(200, "뉴스 생성 성공");
        } catch (Exception e) {
            return RsData.of(500, "뉴스 생성 실패 : " + e.getMessage());
        }
    }

    //뉴스 삭제
    @Operation(summary = "뉴스 삭제", description = "ID로 뉴스를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "뉴스 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 뉴스가 존재하지 않음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{newsId}")
    public RsData<Void> deleteRealNews(@PathVariable Long newsId) {
        boolean deleted = newsDataService.deleteRealNews(newsId);

        if(!deleted){
            return RsData.of(404, String.format("ID %d에 해당하는 뉴스가 존재하지 않습니다", newsId));
        }

        return RsData.of(200, String.format("%d번 뉴스 삭제 완료", newsId));
    }


    @Operation(summary = "오늘의 뉴스 설정", description = "오늘의 뉴스를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "오늘의 뉴스 설정 성공"),
            @ApiResponse(responseCode = "400", description = "이미 오늘의 뉴스로 설정되어 있거나 잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 뉴스가 존재하지 않음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PutMapping("/today/select/{newsId}")
    public RsData<RealNewsDto> setTodayNews(@PathVariable Long newsId) {

        try {
            // 1. 뉴스 존재 여부 확인
            Optional<RealNewsDto> realNewsDto = realNewsService.getRealNewsDtoById(newsId);
            if (realNewsDto.isEmpty()) {
                return RsData.of(404, String.format("ID %d에 해당하는 뉴스가 존재하지 않습니다", newsId));
            }
            // 2. 이미 오늘의 뉴스인지 확인
            if (newsDataService.isAlreadyTodayNews(newsId)) {
                return RsData.of(400, "이미 오늘의 뉴스로 설정되어 있습니다.", realNewsDto.get());
            }

            newsDataService.setTodayNews(newsId);

            return RsData.of(200, "오늘의 뉴스가 설정되었습니다.", realNewsDto.get());

        } catch (IllegalArgumentException e) {
            return RsData.of(400, e.getMessage());
        } catch (Exception e) {
            return RsData.of(500, "오늘의 뉴스 설정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    //단건조회
    @Operation(summary = "단건 뉴스 조회", description = "ID로 단건 뉴스를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "뉴스 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 뉴스 ID"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 뉴스를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{newsId}")
    public RsData<RealNewsDto> getRealNewsById(@PathVariable Long newsId) {

        if (newsId == null || newsId <= 0) {
            return RsData.of(400, "잘못된 뉴스 ID입니다. 1 이상의 숫자를 입력해주세요.");
        }

        Long todayNewsId = realNewsService.getTodayNewsOrRecent();

        if (todayNewsId != -1L && newsId.equals(todayNewsId)) {
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
            @ApiResponse(responseCode = "200", description = "오늘의 뉴스 조회 성공"),
            @ApiResponse(responseCode = "404", description = "조회할 뉴스가 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/today")
    public RsData<RealNewsDto> getTodayNews() {
        Long todayNewsId = realNewsService.getTodayNewsOrRecent();

        if (todayNewsId== -1L) {
            return RsData.of(404, "조회할 뉴스가 없습니다.");
        }

        Optional<RealNewsDto> todayNews = realNewsService.getRealNewsDtoById(todayNewsId);

        return newsPageService.getSingleNews(todayNews, NewsType.REAL, todayNews.get().id());
    }

    //다건조회(시간순)
    @Operation(summary = "관리자용 다건 뉴스 조회", description = "페이지네이션을 통해 시간순으로 다건 뉴스를 조회합니다.")
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

        Sort.Direction sortDirection = fromString(direction);
        Sort sortBy = Sort.by(sortDirection, "originCreatedDate");

        Pageable pageable = PageRequest.of(page-1, size, sortBy);
        Page<RealNewsDto> realNewsPage = realNewsService.getRealNewsListExcludingNth(pageable, 1);

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

        Sort.Direction sortDirection = fromString(direction);
        Sort sortBy = Sort.by(sortDirection, "originCreatedDate");

        Pageable pageable = PageRequest.of(page-1, size, sortBy);
        Page<RealNewsDto> RealNewsPage = realNewsService.searchRealNewsByTitle(title, pageable);

        return newsPageService.getPagedNews(RealNewsPage, NewsType.REAL);
    }

    @Operation(summary = "관리자 카테고리별 뉴스 조회", description = "카테고리별로 모든 뉴스를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카테고리별 뉴스 조회 성공"),
            @ApiResponse(responseCode = "400", description = "올바르지 않은 카테고리이거나 잘못된 페이지 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/category/{category}")
    public RsData<Page<RealNewsDto>> getRealNewsByCategory(
            @Parameter(description = "뉴스 카테고리", example = "ECONOMY",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            allowableValues = {"POLITICS", "ECONOMY", "IT", "CULTURE", "SOCIETY", "NOT_FILTERED"}))
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

        Sort.Direction sortDirection = fromString(direction);
        Sort sortBy = Sort.by(sortDirection, "originCreatedDate");

        Pageable pageable = PageRequest.of(page-1, size, sortBy);
        Page<RealNewsDto> realNewsPage = realNewsService.getAllRealNewsByCategory(newsCategory, pageable);

        return newsPageService.getPagedNews(realNewsPage, NewsType.REAL);
    }

    private boolean isValidPageParam(int page, int size, String direction) {
        return (direction.equals("asc") || direction.equals("desc"))
                && page > 0
                && size >= 1 && size <= 100;
    }
}
