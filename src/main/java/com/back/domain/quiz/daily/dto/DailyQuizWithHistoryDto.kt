package com.back.domain.quiz.daily.dto

import com.back.domain.quiz.QuizType

data class DailyQuizWithHistoryDto(
    val dailyQuizDto: DailyQuizDto, // 사용자가 선택한 답변
    val answer: String?, //정답 여부
    val isCorrect: Boolean, // 경험치 획득량
    val gainExp: Int,
    val quizType: QuizType
)
