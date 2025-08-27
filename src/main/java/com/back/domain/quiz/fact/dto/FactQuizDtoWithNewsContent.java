package com.back.domain.quiz.fact.dto;

import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.fact.entity.CorrectNewsType;
import com.back.domain.quiz.fact.entity.FactQuiz;

public record FactQuizDtoWithNewsContent(
        Long id,
        String question,
        String realNewsTitle,
        String realNewsContent,
        String fakeNewsContent,
        CorrectNewsType correctNewsType,
        QuizType quizType
        ) {
    public FactQuizDtoWithNewsContent(FactQuiz quiz) {
        this(
                quiz.getId(),
                quiz.getQuestion(),
                quiz.getRealNews().getTitle(),
                quiz.getRealNews().getContent(),
                quiz.getFakeNews().getContent(),
                quiz.getCorrectNewsType(),
                quiz.getQuizType()
        );
    }
}
