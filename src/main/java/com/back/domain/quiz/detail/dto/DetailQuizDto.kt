package com.back.domain.quiz.detail.dto

import com.back.domain.quiz.detail.entity.DetailQuiz
import com.back.domain.quiz.detail.entity.Option
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class DetailQuizDto(
    @field:NotBlank val question: String,
    @field:NotBlank val option1: String,
    @field:NotBlank val option2: String,
    @field:NotBlank val option3: String,
    @field:NotNull val correctOption: Option
) {
    constructor(detailQuiz: DetailQuiz) : this(
        detailQuiz.question,
        detailQuiz.option1,
        detailQuiz.option2,
        detailQuiz.option3,
        detailQuiz.correctOption
    )
}

