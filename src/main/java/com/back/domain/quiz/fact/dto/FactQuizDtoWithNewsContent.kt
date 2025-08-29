package com.back.domain.quiz.fact.dto

import com.back.domain.quiz.QuizType
import com.back.domain.quiz.fact.entity.CorrectNewsType
import com.back.domain.quiz.fact.entity.FactQuiz

data class FactQuizDtoWithNewsContent(
    val id: Long,
    val question: String,
    val realNewsTitle: String,
    val realNewsContent: String,
    val fakeNewsContent: String,
    val correctNewsType: CorrectNewsType,
    val quizType: QuizType
) {
    constructor(quiz: FactQuiz) : this(
        quiz.id,
        quiz.question,
        quiz.realNews.title,
        quiz.realNews.content,
        quiz.fakeNews.content,
        quiz.correctNewsType,
        quiz.quizType
    )
}
