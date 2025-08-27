package com.back.domain.member.quizhistory.dto;

import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.back.global.exception.ServiceException;
import lombok.Getter;

@Getter
public class QuizHistoryDto {

    private long id; // 퀴즈 히스토리 ID
    private long quizId; // 퀴즈 ID
    private String quizType; // 퀴즈 타입
    private String createdDate; // 퀴즈 풀이 시간
    private String answer; // 유저 정답
    private boolean isCorrect; // 퀴즈 정답 여부
    private int gainExp; // 퀴즈 풀이로 얻은 경험치

    private long memberId; // 유저 ID
    private String memberName; // 유저 이름


    public QuizHistoryDto(QuizHistory quizHistory) {
        if(quizHistory == null) {
            throw new ServiceException(400, "퀴즈 히스토리가 존재하지 않습니다.");
        }
        if(quizHistory.getMember() == null) {
            throw new ServiceException(400, "퀴즈 히스토리의 유저 정보가 존재하지 않습니다.");
        }

        this.id = quizHistory.getId();
        this.quizId = quizHistory.getQuizId();
        this.quizType = quizHistory.getQuizType().name(); // Enum을 문자열로 변환
        this.answer = quizHistory.getAnswer();
        this.isCorrect = quizHistory.isCorrect();
        this.gainExp = quizHistory.getGainExp();
        this.createdDate = quizHistory.getCreatedDate().toString();
        this.memberId = quizHistory.getMember().getId(); // 유저 ID
        this.memberName = quizHistory.getMember().getName(); // 유저 이름

    }


}
