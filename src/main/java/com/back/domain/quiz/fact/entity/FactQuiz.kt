package com.back.domain.quiz.fact.entity

import com.back.domain.news.fake.entity.FakeNews
import com.back.domain.news.real.entity.RealNews
import com.back.domain.quiz.QuizType
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
class FactQuiz(
    @field:NotBlank(message = "Question can not be blank")
    var question: String,

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "real_news_id", nullable = false)
    @field:NotNull
    @field:JsonIgnore
    var realNews: RealNews,

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "fake_news_id", nullable = false)
    @field:NotNull
    @field:JsonIgnore
    var fakeNews: FakeNews,

    @field:Enumerated(EnumType.STRING)
    @field:NotNull
    var correctNewsType: CorrectNewsType

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Enumerated(EnumType.STRING)
    @NotNull
    val quizType: QuizType = QuizType.FACT

    @CreatedDate
    lateinit var createdDate: LocalDateTime
}