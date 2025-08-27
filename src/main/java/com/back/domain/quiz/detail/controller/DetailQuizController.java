package com.back.domain.quiz.detail.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.quiz.detail.dto.DetailQuizAnswerDto;
import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.dto.DetailQuizResDto;
import com.back.domain.quiz.detail.dto.DetailQuizWithHistoryDto;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.detail.entity.Option;
import com.back.domain.quiz.detail.service.DetailQuizService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz/detail")
@Tag(name = "DetailQuizController", description = "상세 퀴즈 관련 API")
public class DetailQuizController {
    private final DetailQuizService detailQuizService;
    private final Rq rq;

    // 상세 퀴즈 단건 조회(퀴즈 ID로 조회)
    @Operation(summary = "단건 조회", description = "퀴즈 ID로 상세 퀴즈를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상세 퀴즈 조회 성공"),
            @ApiResponse(responseCode = "404", description = "잘못된 퀴즈 ID",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RsData.class),
                            examples = @ExampleObject(value = "{\"resultCode\": 404, \"msg\": \"잘못된 퀴즈 ID입니다.\", \"data\": null}")
                    )
            )}
    )
    @GetMapping("/{id}")
    public RsData<DetailQuizWithHistoryDto> getDetailQuiz(@PathVariable Long id) {

        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }

        DetailQuizWithHistoryDto detailQuiz = detailQuizService.findById(id,actor);

        return new RsData<>(
                200,
                "상세 퀴즈 조회 성공",
                detailQuiz
        );
    }

    // 상세 퀴즈 다건 조회(뉴스 ID로 조회)
    @Operation(summary = "뉴스 ID 기반 다건 조회", description = "뉴스 ID로 해당 뉴스의 상세 퀴즈 목록(3개)을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "뉴스 ID로 상세 퀴즈 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "잘못된 뉴스 ID 또는 해당 뉴스에 퀴즈 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RsData.class),
                            examples = {
                                    @ExampleObject(name = "뉴스 없음", value = "{\"resultCode\": 404, \"msg\": \"해당 id의 뉴스가 존재하지 않습니다. id: \", \"data\": null}"),
                                    @ExampleObject(name = "퀴즈 없음", value = "{\"resultCode\": 404, \"msg\": \"해당 뉴스에 대한 상세 퀴즈가 존재하지 않습니다. newsId: \", \"data\": null}")
                            }
                    )
            )}
    )
    @GetMapping("/news/{newsId}")
    public RsData<List<DetailQuizResDto>> getDetailQuizzesByNewsId(@PathVariable Long newsId) {
        List<DetailQuiz> detailQuizzes = detailQuizService.findByNewsId(newsId);

        return new RsData<>(
                200,
                "뉴스 ID로 상세 퀴즈 목록 조회 성공",
                detailQuizzes.stream()
                        .map(DetailQuizResDto::new)
                        .toList()
        );
    }


    // 상세 퀴즈 생성(뉴스 ID로 찾은 뉴스의 퀴즈 모두 삭제 후 새로 생성해서 저장)
    @Operation(summary = "뉴스 ID 기반 상세 퀴즈 생성", description = "뉴스 ID로 해당 뉴스의 상세 퀴즈를 3개 생성합니다. 기존 퀴즈는 삭제 후 새로 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "상세 퀴즈 생성 성공"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 뉴스를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RsData.class),
                            examples = {
                                    @ExampleObject(value = "{\"resultCode\": 404, \"msg\": \"해당 id의 뉴스가 존재하지 않습니다. id: \", \"data\": null}")
                            }
                    )
            ),
            @ApiResponse(responseCode = "500", description = "AI 서비스 호출 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RsData.class),
                            examples = {
                                    @ExampleObject(value = "{\"resultCode\": 500, \"msg\": \"AI 서비스 호출에 실패했습니다\", \"data\": null}")
                            }
                    )
            )
    }
    )
    @PostMapping("news/{newsId}/regenerate")
    public RsData<List<DetailQuizResDto>> generateDetailQuizzes(@PathVariable Long newsId) {
        List<DetailQuizDto> newQuizzes = detailQuizService.generateQuizzes(newsId);
        List<DetailQuiz> savedQuizzes = detailQuizService.saveQuizzes(newsId, newQuizzes);

        return new RsData<>(
                201,
                "상세 퀴즈 생성 성공",
                savedQuizzes.stream()
                        .map(DetailQuizResDto::new)
                        .toList()
        );
    }


    // 상세 퀴즈 수정(퀴즈 ID로 수정) - 퀴즈 품질 관리를 위한 api
    @Operation(summary = "퀴즈 ID 기반 상세 퀴즈 수정", description = "퀴즈 ID로 상세 퀴즈를 수정합니다.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "상세 퀴즈 수정 성공"),
                    @ApiResponse(responseCode = "404", description = "해당 ID의 상세 퀴즈를 찾을 수 없음",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RsData.class),
                                    examples = {
                                            @ExampleObject(name = "퀴즈 없음", value = "{\"resultCode\": 404, \"msg\": \"해당 id의 상세 퀴즈가 존재하지 않습니다. id: 1\", \"data\": null}")
                                    }))
            }
    )
    @PutMapping("/{id}")
    public RsData<DetailQuizResDto> updateDetailQuiz(@PathVariable Long id, @RequestBody @Valid DetailQuizDto detailQuizDto) {
        DetailQuiz updatedQuiz = detailQuizService.updateDetailQuiz(id, detailQuizDto);
        return new RsData<>(
                200,
                "상세 퀴즈 수정 성공",
                new DetailQuizResDto(updatedQuiz)
        );
    }


    @Operation(summary = "퀴즈 정답 제출", description = "퀴즈 ID로 오늘의 퀴즈의 정답을 제출합니다.")
    @PostMapping("/submit/{id}")
    public RsData<DetailQuizAnswerDto> submitDetailQuizAnswer(@PathVariable Long id, @RequestParam @Valid @NotNull Option selectedOption) {

        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }

        DetailQuizAnswerDto submittedQuiz = detailQuizService.submitDetailQuizAnswer(actor, id, selectedOption);

        return new RsData<>(
                200,
                "퀴즈 정답 제출 성공",
                submittedQuiz
        );
    }

}
