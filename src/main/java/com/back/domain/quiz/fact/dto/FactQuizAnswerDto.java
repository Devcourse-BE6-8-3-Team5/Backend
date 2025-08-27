package com.back.domain.quiz.fact.dto;

import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.fact.entity.CorrectNewsType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FactQuizAnswerDto {
    private Long quizId;
    private String question; //
    private CorrectNewsType selectedNewsType; // 사용자가 선택한 뉴스 타입 (REAL, FAKE)
    private CorrectNewsType correctNewsType; // 정답 뉴스 타입 (REAL, FAKE)

    boolean isCorrect; // 정답 여부
    int gainExp; // 경험치 획득량
    private QuizType quizType; // 퀴즈 타입
}
