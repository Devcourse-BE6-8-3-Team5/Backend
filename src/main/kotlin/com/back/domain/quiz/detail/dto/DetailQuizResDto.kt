package com.back.domain.quiz.detail.dto

import com.back.domain.quiz.detail.entity.DetailQuiz
import com.back.domain.quiz.detail.entity.Option

data class DetailQuizResDto(
    val id: Long?,
    val question: String,
    val option1: String,
    val option2: String,
    val option3: String,
    val correctOption: Option
) {
    constructor(detailQuiz: DetailQuiz) : this(
        detailQuiz.id,
        detailQuiz.question,
        detailQuiz.option1,
        detailQuiz.option2,
        detailQuiz.option3,
        detailQuiz.correctOption
    )
}

