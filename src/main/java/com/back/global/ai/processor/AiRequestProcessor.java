package com.back.global.ai.processor;

import org.springframework.ai.chat.model.ChatResponse;

/**
 * AI 요청을 처리하기 위한 공통 인터페이스입니다.
 * 프롬프트 생성 로직과 응답 결과 파싱 로직은 구현 클래스에서 요청 내용에 따라 정의해주시면 됩니다.
 * DetailQuizProcessor의 구현 로직을 참고하시면 됩니다.
 *
 * @param <T> AI 응답을 파싱하여 반환할 타입
 */

public interface AiRequestProcessor<T> {
    String buildPrompt(); // 요청 프롬프트 생성
    T parseResponse(ChatResponse response); // 응답 파싱
}
