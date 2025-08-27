package com.back.domain.quiz.detail.dto;


import com.back.domain.quiz.QuizType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DetailQuizWithHistoryDto {

    DetailQuizResDto detailQuizResDto; // 상세 퀴즈 정보

    private String answer;// 사용자가 선택한 답변
    boolean isCorrect; //정답 여부
    int gainExp; // 경험치 획득량
    private QuizType quizType;

    public DetailQuizWithHistoryDto(DetailQuizResDto detailQuizResDto, String answer, boolean isCorrect, int gainExp, QuizType quizType) {
        this.detailQuizResDto = detailQuizResDto;
        this.answer = answer;
        this.isCorrect = isCorrect;
        this.gainExp = gainExp;
        this.quizType = quizType;
    }

}
