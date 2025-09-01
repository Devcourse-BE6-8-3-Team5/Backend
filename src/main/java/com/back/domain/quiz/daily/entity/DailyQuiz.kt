package com.back.domain.quiz.daily.entity

import com.back.domain.news.today.entity.TodayNews
import com.back.domain.quiz.QuizType
import com.back.domain.quiz.detail.entity.DetailQuiz
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
class DailyQuiz(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "today_news_id")
    @JsonIgnore
    var todayNews: TodayNews,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detail_quiz_id", unique = true)
    @JsonIgnore
    var detailQuiz: DetailQuiz
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Enumerated(EnumType.STRING)
    @NotNull
    val quizType: QuizType = QuizType.DAILY
}
