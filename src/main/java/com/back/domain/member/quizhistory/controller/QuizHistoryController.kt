package com.back.domain.member.quizhistory.controller

import com.back.domain.member.member.entity.Member
import com.back.domain.member.quizhistory.dto.QuizHistoryDto
import com.back.domain.member.quizhistory.service.QuizHistoryService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/histories")
@Tag(name = "QuizHistoryController", description = "퀴즈 히스토리 API 컨트롤러")
class QuizHistoryController(
    private val quizHistoryService: QuizHistoryService,
    private val rq: Rq
) {
    @GetMapping
    @Operation(summary = "현재 로그인한 유저의 퀴즈 풀이 기록 다건 조회")
    fun getListQuizHistories(): RsData<List<QuizHistoryDto>> {
        val actor: Member? = rq.actor

        if (actor == null) {
            throw ServiceException(401, "로그인이 필요합니다.")
        }

        val histories: List<QuizHistoryDto> = quizHistoryService.getQuizHistoriesByMember(actor)

        return RsData(
            200,
            "퀴즈 히스토리 다건 조회 성공",
            histories
        )
    }
}
