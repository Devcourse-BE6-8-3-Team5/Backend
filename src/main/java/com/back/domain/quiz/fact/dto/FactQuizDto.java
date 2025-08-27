package com.back.domain.quiz.fact.dto;

import com.back.domain.quiz.fact.entity.FactQuiz;

public record FactQuizDto(
        Long id,
        String question,
        String realNewsTitle
) {
    public FactQuizDto(FactQuiz quiz) {
        this(
                quiz.getId(),
                quiz.getQuestion(),
                quiz.getRealNews().getTitle()
        );
    }
}
