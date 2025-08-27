package com.back.domain.quiz.detail.dto;

import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.detail.entity.Option;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DetailQuizAnswerDto {

    private Long quizId;
    private String question; //
    private Option correctOption; // 정답 선택지
    private Option selectedOption; // 사용자가 선택한 답변

    boolean isCorrect; // 정답 여부
    int gainExp; // 경험치 획득량
    private QuizType quizType; // 퀴즈 타입

}
