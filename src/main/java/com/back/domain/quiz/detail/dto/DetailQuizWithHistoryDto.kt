package com.back.domain.quiz.detail.dto

import com.back.domain.quiz.QuizType

data class DetailQuizWithHistoryDto(
    val detailQuizResDto: DetailQuizResDto, // 상세 퀴즈 정보
    val answer: String?,                    // 사용자가 선택한 답변
    val isCorrect: Boolean,                 // 정답 여부
    val gainExp: Int,                       // 경험치 획득량
    val quizType: QuizType                  // 퀴즈 타입
)
