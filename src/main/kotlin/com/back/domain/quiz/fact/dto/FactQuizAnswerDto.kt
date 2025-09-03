package com.back.domain.quiz.fact.dto

import com.back.domain.quiz.QuizType
import com.back.domain.quiz.fact.entity.CorrectNewsType

data class FactQuizAnswerDto(
    val quizId: Long,
    val question: String,
    val selectedNewsType: CorrectNewsType, // 사용자가 선택한 뉴스 타입 (REAL, FAKE)
    val correctNewsType: CorrectNewsType, // 정답 뉴스 타입 (REAL, FAKE)
    val isCorrect: Boolean, // 정답 여부
    val gainExp: Int, // 경험치 획득량
    val quizType: QuizType // 퀴즈 타입
)
