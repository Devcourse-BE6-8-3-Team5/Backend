package com.back.domain.quiz.detail.dto

import com.back.domain.quiz.QuizType
import com.back.domain.quiz.detail.entity.Option

data class DetailQuizAnswerDto(
    val quizId: Long,
    val question: String,
    val correctOption: Option,    // 정답 선택지
    val selectedOption: Option,   // 사용자가 선택한 답변
    val isCorrect: Boolean,       // 정답 여부
    val gainExp: Int,             // 경험치 획득량
    val quizType: QuizType        // 퀴즈 타입
)
