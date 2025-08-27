package com.back.domain.quiz.detail.dto;

import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.detail.entity.Option;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DetailQuizDto(
        @NotBlank String question,
        @NotBlank String option1,
        @NotBlank String option2,
        @NotBlank String option3,
        @NotNull Option correctOption
) {
    public DetailQuizDto(DetailQuiz detailQuiz) {
        this(
                detailQuiz.getQuestion(),
                detailQuiz.getOption1(),
                detailQuiz.getOption2(),
                detailQuiz.getOption3(),
                detailQuiz.getCorrectOption()
        );
    }
}

