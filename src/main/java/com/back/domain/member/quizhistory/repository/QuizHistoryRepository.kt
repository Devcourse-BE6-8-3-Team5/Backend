package com.back.domain.member.quizhistory.repository

import com.back.domain.member.member.entity.Member
import com.back.domain.member.quizhistory.entity.QuizHistory
import com.back.domain.quiz.QuizType
import org.springframework.data.jpa.repository.JpaRepository

interface QuizHistoryRepository : JpaRepository<QuizHistory, Long> {
    fun findByMember(actor: Member): List<QuizHistory>
    fun findByMemberAndQuizTypeAndQuizIdIn(member: Member, quizType: QuizType, quizIds: Set<Long>
    ): MutableList<QuizHistory>
}
