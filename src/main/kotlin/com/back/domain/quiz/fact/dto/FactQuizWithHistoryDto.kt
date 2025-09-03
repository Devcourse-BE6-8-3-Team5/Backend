package com.back.domain.quiz.fact.dto

data class FactQuizWithHistoryDto(
    val factQuizDto: FactQuizDtoWithNewsContent, // 퀴즈 정보 (질문, 정답 뉴스 제목 등)
    val answer: String?, // 사용자가 선택한 답변
    val isCorrect: Boolean, // 정답 여부
    val gainExp: Int // 경험치 획득량
)
