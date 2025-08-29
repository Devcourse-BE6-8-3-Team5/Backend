package com.back.domain.quiz.fact.dto

import com.back.domain.quiz.fact.entity.FactQuiz

data class FactQuizDto(
    val id: Long,
    val question: String,
    val realNewsTitle: String
) {
    constructor(quiz: FactQuiz) : this(
        quiz.id,
        quiz.question,
        quiz.realNews.title
    )
}
