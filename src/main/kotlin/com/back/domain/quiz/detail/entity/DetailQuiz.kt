package com.back.domain.quiz.detail.entity

import com.back.domain.news.real.entity.RealNews
import com.back.domain.quiz.QuizType
import com.back.domain.quiz.daily.entity.DailyQuiz
import com.back.domain.quiz.detail.dto.DetailQuizDto
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Entity
class DetailQuiz(
    @field:NotBlank(message = "Question can not be blank")
    var question: String,

    @field:NotBlank(message = "Option1 can not be blank")
    var option1: String,

    @field:NotBlank(message = "Option2 can not be blank")
    var option2: String,

    @field:NotBlank(message = "Option3 can not be blank")
    var option3: String,

    @field:Enumerated(EnumType.STRING)
    @field:NotNull(message = "Correct option must be specified")
    var correctOption: Option
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "real_news_id", nullable = false)
    @JsonIgnore
    lateinit var realNews: RealNews

    @Enumerated(EnumType.STRING)
    @NotNull
    val quizType: QuizType = QuizType.DETAIL

    // 오늘의 퀴즈와 1:1 관계 설정
    @OneToOne(mappedBy = "detailQuiz", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonIgnore
    val dailyQuiz: DailyQuiz? = null

    val correctAnswerText: String
        // 정답 선택지 텍스트 반환
        get() = when (correctOption) {
            Option.OPTION1 -> option1
            Option.OPTION2 -> option2
            Option.OPTION3 -> option3
        }

    // 정답 판별 메소드
    fun isCorrect(userSelectedOption: Option): Boolean {
        return this.correctOption == userSelectedOption
    }

    constructor(detailQuizDto: DetailQuizDto) : this (
        question = detailQuizDto.question,
        option1 = detailQuizDto.option1,
        option2 = detailQuizDto.option2,
        option3 = detailQuizDto.option3,
        correctOption = detailQuizDto.correctOption
    )
}
