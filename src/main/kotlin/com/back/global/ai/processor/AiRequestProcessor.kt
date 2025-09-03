package com.back.global.ai.processor

import org.springframework.ai.chat.model.ChatResponse

/**
 * AI 요청을 처리하기 위한 공통 인터페이스입니다.
 * 프롬프트 생성 로직과 응답 결과 파싱 로직은 구현 클래스에서 요청 내용에 따라 정의합니다.
 */
interface AiRequestProcessor<T> {
    fun buildPrompt(): String // 요청 프롬프트 생성
    fun parseResponse(response: ChatResponse): T // 응답 파싱
}
