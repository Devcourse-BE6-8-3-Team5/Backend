package com.back.domain.quiz.daily.dto

import com.back.domain.quiz.daily.entity.DailyQuiz
import com.back.domain.quiz.detail.entity.Option

data class DailyQuizDto(
    val id: Long,
    val question: String,
    val option1: String,
    val option2: String,
    val option3: String,
    val correctOption: Option
) {
    constructor(quiz: DailyQuiz) : this(
        quiz.id,
        quiz.detailQuiz.question,
        quiz.detailQuiz.option1,
        quiz.detailQuiz.option2,
        quiz.detailQuiz.option3,
        quiz.detailQuiz.correctOption
    )
}
