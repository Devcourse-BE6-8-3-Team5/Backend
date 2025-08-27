package com.back.domain.quiz.daily.dto;

import com.back.domain.quiz.daily.entity.DailyQuiz;
import com.back.domain.quiz.detail.entity.Option;

public record DailyQuizDto(
        Long id,
        String question,
        String option1,
        String option2,
        String option3,
        Option correctOption
) {
    public DailyQuizDto(DailyQuiz quiz){
        this(
                quiz.getId(),
                quiz.getDetailQuiz().getQuestion(),
                quiz.getDetailQuiz().getOption1(),
                quiz.getDetailQuiz().getOption2(),
                quiz.getDetailQuiz().getOption3(),
                quiz.getDetailQuiz().getCorrectOption()
        );
    }
}
