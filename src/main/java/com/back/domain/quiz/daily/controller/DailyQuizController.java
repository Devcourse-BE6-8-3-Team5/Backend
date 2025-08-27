package com.back.domain.quiz.daily.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.quiz.daily.dto.DailyQuizAnswerDto;
import com.back.domain.quiz.daily.dto.DailyQuizWithHistoryDto;
import com.back.domain.quiz.daily.service.DailyQuizService;
import com.back.domain.quiz.detail.entity.Option;
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
@RequestMapping("/api/quiz/daily")
@RequiredArgsConstructor
@Tag(name= "DailyQuizController", description = "오늘의 퀴즈 관련 API")
public class DailyQuizController {
    private final DailyQuizService dailyQuizService;
    private final Rq rq;

    @Operation(summary = "오늘의 퀴즈 조회", description = "오늘의 뉴스 ID로 오늘의 퀴즈(3개)를 조회합니다.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "오늘의 퀴즈 조회 성공"),
                    @ApiResponse(responseCode = "404", description = "오늘의 뉴스에 해당하는 오늘의 퀴즈가 존재하지 않음",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RsData.class),
                                    examples = @ExampleObject(value = "{\"resultCode\": 404, \"msg\": \"오늘의 뉴스에 해당하는 오늘 퀴즈가 존재하지 않습니다.\", \"data\": null}")))
            }
    )
    @GetMapping("/{todayNewsId}")
    public RsData<List<DailyQuizWithHistoryDto>> getDailyQuizzes(@PathVariable Long todayNewsId) {

        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }

        List<DailyQuizWithHistoryDto> dailyQuizzes = dailyQuizService.getDailyQuizzes(todayNewsId, actor);

        return new RsData<>(
                200,
                "오늘의 퀴즈 조회 성공",
                        dailyQuizzes
        );
    }

    @Operation(summary = "오늘의 퀴즈 정답 제출", description = "퀴즈 ID로 오늘의 퀴즈의 정답을 제출합니다.")
    @PostMapping("/submit/{id}")
    public RsData<DailyQuizAnswerDto> submitDailyQuizAnswer(@PathVariable Long id, @RequestParam @Valid @NotNull Option selectedOption) {

        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }

        DailyQuizAnswerDto submittedQuiz = dailyQuizService.submitDetailQuizAnswer(actor,id, selectedOption);

        return new RsData<>(
                200,
                "퀴즈 정답 제출 성공",
                submittedQuiz
        );
    }
}
