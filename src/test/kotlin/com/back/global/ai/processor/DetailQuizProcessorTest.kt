package com.back.global.ai.processor

import com.back.domain.quiz.detail.dto.DetailQuizCreateReqDto
import com.back.domain.quiz.detail.entity.Option
import com.back.global.exception.ServiceException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation

internal class DetailQuizProcessorTest {
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private lateinit var defaultRequest: DetailQuizCreateReqDto

    @BeforeEach
    fun setUp() {
        defaultRequest = DetailQuizCreateReqDto("테스트 뉴스 제목", "테스트 뉴스 본문 내용입니다.")
    }

    @Test
    @DisplayName("buildPrompt는 req의 title과 content를 포함해야 한다.")
    fun t1() {
        // given
        val processor = DetailQuizProcessor(defaultRequest, objectMapper)

        // when
        val prompt = processor.buildPrompt()

        // then
        assertThat(prompt).contains("테스트 뉴스 제목", "테스트 뉴스 본문 내용입니다.", "객관식 퀴즈 3개")
    }

    @Test
    @DisplayName("parseResponse는 3개의 DetailQuizDto를 반환해야 한다.")
    fun t2() {
        // given
        val processor = DetailQuizProcessor(defaultRequest, objectMapper)

        val mockJson = """
            [
              {
                "question": "Qwen3-Coder는 어떤 회사가 공개했나요?",
                "option1": "알리바바",
                "option2": "구글",
                "option3": "메타",
                "correctOption": "OPTION1"
              },
              {
                "question": "Qwen3-Coder가 지원하는 최대 컨텍스트 길이는?",
                "option1": "10만",
                "option2": "100만",
                "option3": "1천만",
                "correctOption": "OPTION2"
              },
              {
                "question": "Qwen3-Coder의 특화 분야는 무엇인가요?",
                "option1": "소프트웨어 개발 지원",
                "option2": "음성 인식",
                "option3": "자율 주행",
                "correctOption": "OPTION1"
              }
            ]
        
        """.trimIndent()

        // ChatResponse mock 생성
        val mockResponse = createMockChatResponse(mockJson)

        // when
        val result = processor.parseResponse(mockResponse)

        // then
        assertThat(result).hasSize(3)

        assertThat(result[0].question).isEqualTo("Qwen3-Coder는 어떤 회사가 공개했나요?")
        assertThat(result[0].option1).isEqualTo("알리바바")
        assertThat(result[0].option2).isEqualTo("구글")
        assertThat(result[0].option3).isEqualTo("메타")
        assertThat(result[0].correctOption).isEqualTo(Option.OPTION1)

        assertThat(result[1].correctOption).isEqualTo(Option.OPTION2)
    }

    @Test
    @DisplayName("parseResponse는 null 응답에 대해 예외를 던져야 한다.")
    fun t3() {
        // given
        val processor = DetailQuizProcessor(defaultRequest, objectMapper)
        val mockResponse = createMockChatResponse(null)

        // when & then
        assertThatThrownBy { processor.parseResponse(mockResponse) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining("AI 응답이 비어있습니다")
    }

    @Test
    @DisplayName("parseResponse는 응답 결과가 json 형식이 아니면 예외를 던져야 한다.")
    fun t4() {
        // given
        val processor = DetailQuizProcessor(defaultRequest, objectMapper)

        val mockResponse = createMockChatResponse("INVALID_JSON")

        // when & then
        assertThatThrownBy { processor.parseResponse(mockResponse) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining("JSON 형식이 아닙니다")
    }

    @Test
    @DisplayName("parseResponse는 응답 결과 생성된 퀴즈가 3개가 아니면 예외를 던져야 한다.")
    fun t5() {
        // given
        val processor = DetailQuizProcessor(defaultRequest, objectMapper)

        val mockJson = """
            [
              {
                "question": "하나만 있는 퀴즈",
                "option1": "정답",
                "option2": "오답1",
                "option3": "오답2",
                "correctOption": "OPTION1"
              }
            ]
        
        """.trimIndent()

        val mockResponse = createMockChatResponse(mockJson)

        // when & then
        assertThatThrownBy { processor.parseResponse(mockResponse) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining("3개의 퀴즈")
    }

    //ChatResponse 목 객체를 생성하는 헬퍼 메서드 - 명시적 모킹
    private fun createMockChatResponse(responseText: String?) = mock(ChatResponse::class.java).apply {
        val generation = mock(Generation::class.java)
        val output = mock(AssistantMessage::class.java)
        `when`(result).thenReturn(generation)
        `when`(generation.output).thenReturn(output)
        `when`(output.text).thenReturn(responseText)
    }
}

