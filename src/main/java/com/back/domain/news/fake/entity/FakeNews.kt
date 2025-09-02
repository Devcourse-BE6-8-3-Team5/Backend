package com.back.domain.news.fake.entity

import com.back.domain.news.real.entity.RealNews
import com.back.domain.quiz.fact.entity.FactQuiz
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "fake_news")
class FakeNews(
    @field:Id
    @field:Column(name = "real_news_id")
    val id: Long,

    @field:OneToOne
    @field:MapsId
    @field:JoinColumn(name = "real_news_id")
    @get:JsonIgnore
    val realNews: RealNews,

    @field:Lob
    @field:Column(nullable = false)
    val content: String
) {
    @field:OneToMany(mappedBy = "fakeNews", cascade = [CascadeType.ALL], orphanRemoval = true)
    @get:JsonIgnore
    val factQuizzes: List<FactQuiz> = mutableListOf()

}
