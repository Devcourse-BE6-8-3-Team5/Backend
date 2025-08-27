package com.back.domain.quiz.fact.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.news.real.service.NewsDataService;
import com.back.domain.news.real.service.RealNewsService;
import com.back.domain.quiz.fact.dto.FactQuizAnswerDto;
import com.back.domain.quiz.fact.dto.FactQuizDto;
import com.back.domain.quiz.fact.dto.FactQuizWithHistoryDto;
import com.back.domain.quiz.fact.entity.CorrectNewsType;
import com.back.domain.quiz.fact.entity.FactQuiz;
import com.back.domain.quiz.fact.service.FactQuizService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz/fact")
@Tag(name = "FactQuizController", description = "팩트 퀴즈(진짜가짜 퀴즈) 관련 API")
public class FactQuizController {
    private final FactQuizService factQuizService;
    private final Rq rq;
    private static final int DEFAULT_RANK = 2; // 기본 랭크 값

    @Operation(summary = "팩트 퀴즈 전체 조회", description = "팩트 퀴즈 (전체) 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팩트 퀴즈 (전체) 목록 조회 성공")
    })
    @GetMapping
    public RsData<List<FactQuizDto>> getFactQuizzes() {
        List<FactQuizDto> factQuizzes = factQuizService.findByRank(DEFAULT_RANK);

        return new RsData<>(
                200,
                "팩트 퀴즈 목록 조회 성공",
                factQuizzes
        );
    }

    @Operation(summary = "팩트 퀴즈 카테고리별 조회", description = "카테고리별로 팩트 퀴즈 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팩트 퀴즈 목록 조회 성공")
    })
    @GetMapping("/category")
    public RsData<List<FactQuizDto>> getFactQuizzesByCategory(@RequestParam NewsCategory category) {
        List<FactQuizDto> factQuizzes = factQuizService.findByCategory(category, DEFAULT_RANK);

        return new RsData<>(
                200,
                "팩트 퀴즈 목록 조회 성공. 카테고리: " + category,
                factQuizzes
        );
    }

    @Operation(summary = "팩트 퀴즈 단건 조회", description = "팩트 퀴즈 ID로 팩트 퀴즈를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팩트 퀴즈 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 팩트 퀴즈를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RsData.class),
                            examples = @ExampleObject(value = "{\"resultCode\": 404, \"msg\": \"팩트 퀴즈를 찾을 수 없습니다. ID: 1\", \"data\": null}"))),
    })
    @GetMapping("/{id}")
    public RsData<FactQuizWithHistoryDto> getFactQuizById(@PathVariable Long id) {

        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }

        FactQuizWithHistoryDto factQuiz = factQuizService.findById(id,actor);

        return new RsData<>(
                200,
                "팩트 퀴즈 조회 성공. ID: " + id,
                factQuiz
        );
    }

    @Operation(summary = "팩트 퀴즈 삭제", description = "팩트 퀴즈 ID로 팩트 퀴즈를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팩트 퀴즈 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "팩트 퀴즈를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RsData.class),
                            examples = @ExampleObject(value = "{\"resultCode\": 404, \"msg\": \"팩트 퀴즈를 찾을 수 없습니다. ID: 1\", \"data\": null}")))
    })
    @DeleteMapping("/{id}")
    public RsData<Void> deleteFactQuiz(@PathVariable Long id) {
        factQuizService.delete(id);
        return RsData.of(
                200,
                "팩트 퀴즈 삭제 성공. ID: " + id);
    }

    @Operation(summary = "팩트 체크 퀴즈 정답 제출", description = """
                                                    팩트 체크 퀴즈 ID로 팩트 체크 퀴즈의 정답을 제출합니다.
                                                    데이터 보낼시 프론트에서 타입을 보내줘야합니다 
                                                    {
                                                        "selectedNewsType": "REAL" 또는 "FAKE"
                                                    }
                                                    """ )
    @PostMapping("/submit/{id}")
    @Transactional
    public RsData<FactQuizAnswerDto> submitFactQuizAnswer(@PathVariable Long id, @RequestParam @Valid @NotNull CorrectNewsType selectedNewsType) {

        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }

        FactQuizAnswerDto submittedQuiz = factQuizService.submitDetailQuizAnswer(actor,id, selectedNewsType);

        return new RsData<>(
                200,
                "퀴즈 정답 제출 성공",
                submittedQuiz
        );
    }

}

