package com.back.global.ai

import com.back.global.ai.processor.AiRequestProcessor
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class AiService(
    private val chatClient: ChatClient,
) {

    /**
     * 공통 AI 요청을 처리하는 메서드입니다.
     * 프롬프트 생성 및 응답 파싱 로직을 AiRequestProcessor 구현체에 위임합니다.
     *
     * @param processor 프롬프트 생성 및 응답 파싱을 담당하는 프로세서 객체
     * @param <T> 프로세서가 반환하는 타입(List<DTO> 또는 단일 DTO)
     * @return AI 응답 결과를 파싱한 객체
     */
    fun <T> process(processor: AiRequestProcessor<T>): T {
        val prompt = processor.buildPrompt() // 프롬프트 생성

        val response = chatClient.prompt(prompt)
            .call()
            .chatResponse()

        return processor.parseResponse(response) // AI 응답 파싱
    }
}
