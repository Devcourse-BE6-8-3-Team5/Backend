package com.back.domain.member.quizhistory.service

import com.back.domain.member.member.entity.Member
import com.back.domain.member.quizhistory.dto.QuizHistoryDto
import com.back.domain.member.quizhistory.entity.QuizHistory
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository
import com.back.domain.quiz.QuizType
import com.back.global.exception.ServiceException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QuizHistoryService(private val quizHistoryRepository: QuizHistoryRepository) {

    @Transactional(readOnly = true)
    fun getQuizHistoriesByMember(actor: Member): List<QuizHistoryDto> {
        val quizHistories = quizHistoryRepository.findByMember(actor)
            .sortedBy {it.quizType}

        return quizHistories.map { QuizHistoryDto(it) }
    }

    @Transactional
    fun save(actor: Member, id: Long?, quizType: QuizType, answer: String, isCorrect: Boolean, gainExp: Int) {
        val quizHistory = QuizHistory(
            actor,
            id,
            quizType,
            answer,
            isCorrect,
            gainExp
        )

        // 퀴즈 히스토리 저장
        try {
            quizHistoryRepository.save(quizHistory)
        } catch (e: DataIntegrityViolationException) {
            throw ServiceException(400, "이미 푼 문제입니다.")
        }
    }
}
