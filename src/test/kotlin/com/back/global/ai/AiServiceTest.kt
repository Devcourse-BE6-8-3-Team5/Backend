package com.back.global.ai

import com.back.domain.quiz.detail.dto.DetailQuizCreateReqDto
import com.back.domain.quiz.detail.entity.Option
import com.back.global.ai.processor.DetailQuizProcessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatResponse

class AiServiceTest {
    @Test
    @DisplayName("process(with DetailQuizProcessor)는 파싱된 결과값을 반환해야 한다")
    fun t1() {
        // given

        val mockChatClient = Mockito.mock(ChatClient::class.java, Mockito.RETURNS_DEEP_STUBS)
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

        // Mock ChatResponse
        val mockResponse = Mockito.mock(ChatResponse::class.java, Mockito.RETURNS_DEEP_STUBS)
        Mockito.`when`(mockResponse.result.output.text).thenReturn(
            """
            [
              {
                "question": "알리바바가 발표한 모델 이름은?",
                "option1": "Qwen3-Coder",
                "option2": "GPT-4",
                "option3": "Claude 4",
                "correctOption": "OPTION1"
              },
              {
                "question": "이 모델이 지원하는 최대 토큰 길이는?",
                "option1": "1만",
                "option2": "10만",
                "option3": "100만",
                "correctOption": "OPTION3"
              },
              {
                "question": "Qwen3-Coder의 주요 특화 분야는?",
                "option1": "소프트웨어 개발 지원",
                "option2": "이미지 생성",
                "option3": "음악 작곡",
                "correctOption": "OPTION1"
              }
            ]
        
        """.trimIndent()
        )

        Mockito.`when`(mockChatClient.prompt(ArgumentMatchers.anyString()).call().chatResponse())
            .thenReturn(mockResponse)

        val aiService = AiService(mockChatClient)

        val processor = DetailQuizProcessor(
            DetailQuizCreateReqDto("제목", "본문"),
            objectMapper
        )

        // when
        val result = aiService.process(processor)

        // then
        println(result)
        Assertions.assertThat(result).hasSize(3)

        // 첫 번째 퀴즈 검증
        val firstQuiz = result[0]
        Assertions.assertThat(firstQuiz.question).isEqualTo("알리바바가 발표한 모델 이름은?")
        Assertions.assertThat(firstQuiz.option1).isEqualTo("Qwen3-Coder")
        Assertions.assertThat(firstQuiz.option2).isEqualTo("GPT-4")
        Assertions.assertThat(firstQuiz.option3).isEqualTo("Claude 4")
        Assertions.assertThat(firstQuiz.correctOption).isEqualTo(Option.OPTION1)

        // 두 번째, 세 번째 퀴즈 정답값 확인 (다양성 검증)
        Assertions.assertThat(result[1].correctOption).isEqualTo(Option.OPTION3)
        Assertions.assertThat(result[2].correctOption).isEqualTo(Option.OPTION1)
    }
}

