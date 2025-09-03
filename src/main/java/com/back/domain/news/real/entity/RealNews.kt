package com.back.domain.news.real.entity

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.fake.entity.FakeNews
import com.back.domain.news.today.entity.TodayNews
import com.back.domain.quiz.detail.entity.DetailQuiz
import com.back.domain.quiz.fact.entity.FactQuiz
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime


@Entity
@Table(
    name = "real_news",
    indexes = [
        Index(name = "idx_real_news_category_created_date",columnList = "news_category, created_date DESC"),
        Index(name = "idx_real_news_created_date_desc",columnList = "created_date DESC"),
        Index(name = "idx_real_news_origin_created_date_desc",columnList = "origin_created_date DESC"),
        Index(name = "idx_real_news_category_origin_created_date_desc", columnList = "news_category, origin_created_date DESC")
    ]
)
data class RealNews(
    @field:Lob
    val content: String,
    val title: String,
    val description: String,
    val link: String,
    val imgUrl: String,
    val originCreatedDate: LocalDateTime,
    val mediaName: String,
    val journalist: String,
    val originalNewsUrl: String,

    @field:Column(updatable = false)
    @field:CreationTimestamp
    val createdDate: LocalDateTime,

    @field:Enumerated(EnumType.STRING)
    val newsCategory: NewsCategory,

    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
) { // 상세 퀴즈와 1:N 관계 설정 (RealNews 하나 당 3개의 DetailQuiz가 생성됩니다.)
    @field:OneToMany(mappedBy = "realNews", cascade = [CascadeType.ALL], orphanRemoval = true)
    @get:JsonIgnore
    val detailQuizzes: MutableList<DetailQuiz> = mutableListOf()

    @field:OneToMany(mappedBy = "realNews", cascade = [CascadeType.ALL], orphanRemoval = true)
    @get:JsonIgnore
    val factQuizzes: List<FactQuiz> = mutableListOf()

    @field:OneToOne(mappedBy = "realNews", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @get:JsonIgnore
    var fakeNews: FakeNews? = null

    @field:OneToOne(mappedBy = "realNews", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @get:JsonIgnore
    var todayNews: TodayNews? = null
}
