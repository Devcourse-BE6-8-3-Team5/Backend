package com.back.domain.member.quizhistory.controller;


import com.back.domain.member.member.entity.Member;
import com.back.domain.member.quizhistory.dto.QuizHistoryDto;
import com.back.domain.member.quizhistory.service.QuizHistoryService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/histories")
@RequiredArgsConstructor
@Tag(name = "QuizHistoryController", description = "퀴즈 히스토리 API 컨트롤러")
public class QuizHistoryController {

    private final QuizHistoryService quizHistoryService;
    private final Rq rq;

    @GetMapping
    @Operation(summary = "현재 로그인한 유저의 퀴즈 풀이 기록 다건 조회")
    public RsData<List<QuizHistoryDto>> getListQuizHistories() {
        Member actor = rq.getActor();

        if(actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }

        List<QuizHistoryDto> histories = quizHistoryService.getQuizHistoriesByMember(actor);

        return new RsData<>(
                200,
                "퀴즈 히스토리 다건 조회 성공",
                histories
        );
    }

}
