package com.back.domain.news.today.entity

import com.back.domain.news.real.entity.RealNews
import com.back.domain.quiz.daily.entity.DailyQuiz
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDate

@Entity
class TodayNews(
    var selectedDate: LocalDate,

    @field:JsonIgnore
    @field:JoinColumn(name = "real_news_id")
    @field:MapsId
    @field:OneToOne(fetch = FetchType.LAZY)
    var realNews: RealNews
) {
    @Id
    val id: Long = 0

    // 오늘의 퀴즈와 1:N 관계 설정
    @field:OneToMany(mappedBy = "todayNews", cascade = [CascadeType.ALL], orphanRemoval = true)
    @field:JsonIgnore
    val todayQuizzes: MutableList<DailyQuiz> = mutableListOf()
}