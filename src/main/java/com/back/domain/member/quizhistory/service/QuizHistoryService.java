package com.back.domain.member.quizhistory.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.quizhistory.dto.QuizHistoryDto;
import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository;
import com.back.domain.quiz.QuizType;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizHistoryService {

    private final QuizHistoryRepository quizHistoryRepository;

    @Transactional(readOnly = true)
    public List<QuizHistoryDto> getQuizHistoriesByMember(Member actor) {
        List<QuizHistory> quizHistories = quizHistoryRepository.findByMember(actor);

        quizHistories.sort(Comparator.comparing(QuizHistory::getQuizType)); // 퀴즈타입별 정렬


        return quizHistories.stream()
                .map(QuizHistoryDto::new)
                .toList();
    }

    @Transactional
    public void save(Member actor, Long id,QuizType quizType, String answer, boolean isCorrect, int gainExp) {

        QuizHistory quizHistory = QuizHistory.builder()
                .member(actor)
                .quizId(id)
                .quizType(quizType)
                .answer(answer)
                .isCorrect(isCorrect)
                .gainExp(gainExp)
                .build();

        // 퀴즈 히스토리 저장
        try {
            quizHistoryRepository.save(quizHistory);
        } catch (DataIntegrityViolationException e) {
            throw new ServiceException(400, "이미 푼 문제입니다.");
        }
    }
}
