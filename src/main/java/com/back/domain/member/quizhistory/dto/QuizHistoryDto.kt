package com.back.domain.member.quizhistory.dto

import com.back.domain.member.quizhistory.entity.QuizHistory
import com.back.global.exception.ServiceException

class QuizHistoryDto(
    val id: Long,
    val quizId: Long?,
    val quizType: String,
    val createdDate: String,
    val answer: String,
    val isCorrect: Boolean,
    val gainExp: Int,
    val memberId: Long,
    val memberName: String?
) {

    constructor(quizHistory: QuizHistory?) : this(
        id = quizHistory?.id ?: throw ServiceException(400, "퀴즈 히스토리가 존재하지 않습니다."),
        quizId = quizHistory.quizId,
        quizType = quizHistory.quizType.name,
        answer = quizHistory.answer,
        isCorrect = quizHistory.isCorrect,
        gainExp = quizHistory.gainExp,
        createdDate = quizHistory.createdDate.toString(),
        memberId = quizHistory.member?.id ?: throw ServiceException(400, "퀴즈 히스토리의 유저 정보가 존재하지 않습니다."),
        memberName = quizHistory.member?.name
    )
}
