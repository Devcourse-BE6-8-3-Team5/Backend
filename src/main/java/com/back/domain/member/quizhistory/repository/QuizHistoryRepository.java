package com.back.domain.member.quizhistory.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.back.domain.quiz.QuizType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {

    List<QuizHistory> findByMember(Member actor);
    List<QuizHistory> findByMemberAndQuizTypeAndQuizIdIn(Member member, QuizType quizType, Set<Long> quizIds);
}
