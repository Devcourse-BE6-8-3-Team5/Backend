package com.back.domain.quiz.daily.entity

import com.back.domain.news.today.entity.TodayNews
import com.back.domain.quiz.QuizType
import com.back.domain.quiz.detail.entity.DetailQuiz
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
class DailyQuiz(
    @field:ManyToOne(fetch = FetchType.LAZY, optional = false)
    @field:JoinColumn(name = "today_news_id")
    @field:JsonIgnore
    var todayNews: TodayNews,

    @field:OneToOne(fetch = FetchType.LAZY, optional = false)
    @field:JoinColumn(name = "detail_quiz_id", unique = true)
    @field:JsonIgnore
    var detailQuiz: DetailQuiz
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Enumerated(EnumType.STRING)
    @NotNull
    val quizType: QuizType = QuizType.DAILY
}
