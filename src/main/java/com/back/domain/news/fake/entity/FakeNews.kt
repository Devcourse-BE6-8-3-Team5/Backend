package com.back.domain.news.fake.entity

import com.back.domain.news.real.entity.RealNews
import com.back.domain.quiz.fact.entity.FactQuiz
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(
    name = "fake_news",
    indexes = [
        Index(name = "idx_fake_news_real_news_id", columnList = "real_news_id", unique = true)
    ]
)
class FakeNews(
    @field:OneToOne(fetch = FetchType.LAZY)
    @field:MapsId
    @field:JoinColumn(name = "real_news_id")
    @get:JsonIgnore
    val realNews: RealNews,

    @field:Lob
    @field:Column(nullable = false)
    val content: String
) {
    @field:Id
    @field:Column(name = "real_news_id")
    val id: Long = 0L // @MapsId가 자동으로 설정하므로 초기값만

    @field:OneToMany(mappedBy = "fakeNews", cascade = [CascadeType.ALL], orphanRemoval = true)
    @get:JsonIgnore
    val factQuizzes: List<FactQuiz> = mutableListOf()
}