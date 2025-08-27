package com.back.domain.quiz.fact.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FactQuizWithHistoryDto {

    private FactQuizDtoWithNewsContent factQuizDto; // 퀴즈 정보 (질문, 정답 뉴스 제목 등)

    private String answer; // 사용자가 선택한 답변
    private boolean isCorrect; // 정답 여부
    private int gainExp; // 경험치 획득량

    public FactQuizWithHistoryDto(FactQuizDtoWithNewsContent factQuizDto, String answer, boolean isCorrect, int gainExp) {
        this.factQuizDto = factQuizDto;
        this.answer = answer;
        this.isCorrect = isCorrect;
        this.gainExp = gainExp;
    }

}
