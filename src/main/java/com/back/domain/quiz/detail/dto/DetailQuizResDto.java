package com.back.domain.quiz.detail.dto;

import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.detail.entity.Option;

public record DetailQuizResDto(
        Long id,
        String question,
        String option1,
        String option2,
        String option3,
        Option correctOption
) {
    public DetailQuizResDto(DetailQuiz detailQuiz) {
        this(
                detailQuiz.getId(),
                detailQuiz.getQuestion(),
                detailQuiz.getOption1(),
                detailQuiz.getOption2(),
                detailQuiz.getOption3(),
                detailQuiz.getCorrectOption()
        );
    }
}

