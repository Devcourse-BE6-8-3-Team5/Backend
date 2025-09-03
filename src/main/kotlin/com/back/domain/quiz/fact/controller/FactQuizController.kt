package com.back.domain.quiz.fact.controller

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.quiz.fact.dto.FactQuizAnswerDto
import com.back.domain.quiz.fact.dto.FactQuizDto
import com.back.domain.quiz.fact.dto.FactQuizWithHistoryDto
import com.back.domain.quiz.fact.entity.CorrectNewsType
import com.back.domain.quiz.fact.service.FactQuizService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/quiz/fact")
@Tag(name = "FactQuizController", description = "팩트 퀴즈(진짜가짜 퀴즈) 관련 API")
class FactQuizController(
    private val factQuizService: FactQuizService,
    private val rq: Rq
) {
    companion object {
        private const val DEFAULT_RANK = 2 // 기본 랭크 값
    }

    @Operation(summary = "팩트 퀴즈 전체 조회", description = "팩트 퀴즈 (전체) 목록을 조회합니다.")
    @ApiResponses(ApiResponse(responseCode = "200", description = "팩트 퀴즈 (전체) 목록 조회 성공"))
    @GetMapping
    fun getFactQuizzes(): RsData<List<FactQuizDto>> {
        val factQuizzes = factQuizService.findByRank(DEFAULT_RANK)
        return RsData(200, "팩트 퀴즈 목록 조회 성공", factQuizzes)
    }

    @Operation(summary = "팩트 퀴즈 카테고리별 조회", description = "카테고리별로 팩트 퀴즈 목록을 조회합니다.")
    @ApiResponses(ApiResponse(responseCode = "200", description = "팩트 퀴즈 목록 조회 성공"))
    @GetMapping("/category")
    fun getFactQuizzesByCategory(@RequestParam category: NewsCategory): RsData<List<FactQuizDto>> {
        val factQuizzes = factQuizService.findByCategory(category, DEFAULT_RANK)
        return RsData(200, "팩트 퀴즈 목록 조회 성공. 카테고리: $category", factQuizzes)
    }

    @Operation(summary = "팩트 퀴즈 단건 조회", description = "팩트 퀴즈 ID로 팩트 퀴즈를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "팩트 퀴즈 조회 성공"),
        ApiResponse(
            responseCode = "404", description = "해당 ID의 팩트 퀴즈를 찾을 수 없음",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = RsData::class),
                examples = [ExampleObject(value = """{"code":404,"msg":"팩트 퀴즈를 찾을 수 없습니다. ID: 1","data":null}""")]
            )]
        )
    )
    @GetMapping("/{id}")
    fun getFactQuizById(@PathVariable id: Long): RsData<FactQuizWithHistoryDto> {
        val actor = rq.actor ?: throw ServiceException(401, "로그인이 필요합니다.")
        val factQuiz = factQuizService.findById(id, actor)
        return RsData(200, "팩트 퀴즈 조회 성공. ID: $id", factQuiz)
    }

    @Operation(summary = "팩트 퀴즈 삭제", description = "팩트 퀴즈 ID로 팩트 퀴즈를 삭제합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "팩트 퀴즈 삭제 성공"),
        ApiResponse(
            responseCode = "404", description = "팩트 퀴즈를 찾을 수 없음",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = RsData::class),
                examples = [ExampleObject(value = """{"code":404,"msg":"팩트 퀴즈를 찾을 수 없습니다. ID: 1","data":null}""")]
            )]
        )
    )
    @DeleteMapping("/{id}")
    fun deleteFactQuiz(@PathVariable id: Long): RsData<Void?> {
        factQuizService.delete(id)
        return RsData.of(200, "팩트 퀴즈 삭제 성공. ID: $id")
    }

    @Operation(
        summary = "팩트 체크 퀴즈 정답 제출",
        description = """
            팩트 체크 퀴즈 ID로 팩트 체크 퀴즈의 정답을 제출합니다.
            데이터 보낼시 프론트에서 타입을 보내줘야합니다 
            {
                "selectedNewsType": "REAL" 또는 "FAKE"
            }
        """
    )
    @PostMapping("/submit/{id}")
    @Transactional
    fun submitFactQuizAnswer(
        @PathVariable id: Long,
        @RequestParam @Valid @NotNull selectedNewsType: CorrectNewsType
    ): RsData<FactQuizAnswerDto> {
        val actor = rq.actor ?: throw ServiceException(401, "로그인이 필요합니다.")
        val submittedQuiz = factQuizService.submitDetailQuizAnswer(actor, id, selectedNewsType)
        return RsData(200, "퀴즈 정답 제출 성공", submittedQuiz)
    }
}

