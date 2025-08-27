package com.back.domain.quiz.detail.entity;

import com.back.domain.news.real.entity.RealNews;
import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.daily.entity.DailyQuiz;
import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DetailQuiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Question can not be blank")
    private String question;

    @NotBlank(message = "Option1 can not be blank")
    private String option1;
    @NotBlank(message = "Option2 can not be blank")
    private String option2;
    @NotBlank(message = "Option3 can not be blank")
    private String option3;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Correct option must be specified")
    private Option correctOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "real_news_id")
    @JsonIgnore
    private RealNews realNews;

    @Enumerated(EnumType.STRING)
    @NotNull
    private QuizType quizType = QuizType.DETAIL;

    // 오늘의 퀴즈와 1:1 관계 설정
    @OneToOne(mappedBy = "detailQuiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private DailyQuiz dailyQuiz;


    // 정답 선택지 텍스트 반환
    public String getCorrectAnswerText() {
        if (correctOption == null) {
            return null; // global exception handler 추가되면 적절한 예외 던지게 수정
        }
        return switch (correctOption) {
            case OPTION1 -> option1;
            case OPTION2 -> option2;
            case OPTION3 -> option3;
        };
    }

    // 정답 판별 메소드
    public boolean isCorrect(Option userSelectedOption) {
        return this.correctOption == userSelectedOption;
    }

    public DetailQuiz(String question, String option1, String option2, String option3, Option correctOption) {
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.correctOption = correctOption;
    }

    public DetailQuiz(DetailQuizDto detailQuizDto) {
        this.question = detailQuizDto.question();
        this.option1 = detailQuizDto.option1();
        this.option2 = detailQuizDto.option2();
        this.option3 = detailQuizDto.option3();
        this.correctOption = detailQuizDto.correctOption();
    }

}
