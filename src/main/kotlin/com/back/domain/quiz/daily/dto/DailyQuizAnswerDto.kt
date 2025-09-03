package com.back.domain.quiz.daily.dto

import com.back.domain.quiz.QuizType
import com.back.domain.quiz.detail.entity.Option

data class DailyQuizAnswerDto(
    val quizId: Long,
    val question: String,
    val correctOption: Option,
    var selectedOption: Option,
    var isCorrect: Boolean,
    var gainExp: Int,
    val quizType: QuizType
)
