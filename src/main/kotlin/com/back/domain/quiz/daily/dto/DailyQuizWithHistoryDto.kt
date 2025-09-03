package com.back.domain.quiz.daily.dto

import com.back.domain.quiz.QuizType

data class DailyQuizWithHistoryDto(
    val dailyQuizDto: DailyQuizDto,
    val answer: String?, // 사용자가 선택한 답변
    val isCorrect: Boolean, //정답 여부
    val gainExp: Int, // 경험치 획득량
    val quizType: QuizType
)
