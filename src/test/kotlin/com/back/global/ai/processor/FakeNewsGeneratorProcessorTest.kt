package com.back.global.ai.processor

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.fake.dto.FakeNewsDto
import com.back.domain.news.real.dto.RealNewsDto
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import java.time.LocalDateTime
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeNewsGeneratorProcessorTest {

    private lateinit var processor: FakeNewsGeneratorProcessor
    private val objectMapper = ObjectMapper()
    private val mockChatResponse = mockk<ChatResponse>()
    private val sampleRealNewsDto = RealNewsDto(
        id = 123L,
        title = "삼성전자, AI 반도체 개발 성공으로 주가 급등",
        content = "삼성전자가 차세대 AI 반도체 개발에 성공했다고 15일 발표했다. 이번 반도체는 기존 대비 처리 속도가 50% 향상되었으며, 전력 소모량은 30% 감소했다. 삼성전자 관계자는 \"이번 기술로 글로벌 AI 시장에서 경쟁력을 크게 높일 것\"이라고 말했다. 이 소식에 삼성전자 주가는 전일 대비 5.2% 상승한 7만2천원에 거래를 마감했다.",
        description = "삼성전자 AI 반도체 개발 성공, 주가 급등",
        link = "https://news.example.com/samsung-ai-chip",
        imgUrl = "https://img.example.com/samsung.jpg",
        originCreatedDate = LocalDateTime.of(2025, 8, 15, 14, 30),
        createdDate = LocalDateTime.of(2025, 8, 15, 15, 0),
        mediaName = "테스트뉴스",
        journalist = "김기자",
        originalNewsUrl = "https://original.example.com/news/123",
        newsCategory = NewsCategory.IT
    )

    @BeforeEach
    fun setUp() {
        processor = FakeNewsGeneratorProcessor(sampleRealNewsDto, objectMapper)
    }

    @Test
    @DisplayName("정상적인 AI 응답 파싱 성공 테스트")
    fun `should parse valid AI response successfully`() {
        // given
        mockChatResponseWithText(VALID_JSON_RESPONSE)

        // when
        val result: FakeNewsDto = processor.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(result.realNewsId).isEqualTo(123L)
            assertThat(result.content).isEqualTo("Apple이 차세대 M3 프로세서를 발표했다고 16일 공식 발표했다. 새로운 프로세서는 이전 세대 대비 60% 빠른 처리 속도를 자랑하며, 배터리 효율성도 40% 개선됐다. Apple 대변인은 \"혁신적인 기술로 모바일 컴퓨팅의 새 지평을 열겠다\"고 밝혔다. 이에 따라 Apple 주식은 장중 6.8% 급등하며 180달러를 돌파했다.")
            assertThat(result.content.length).isBetween(150, 250) // 적절한 길이 범위
        }
    }

    @Test
    @DisplayName("JSON 마크다운 블록이 있는 응답 파싱 테스트")
    fun `should parse JSON response with markdown blocks`() {
        // given
        mockChatResponseWithText(JSON_WITH_MARKDOWN)

        // when
        val result: FakeNewsDto = processor.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(result.realNewsId).isEqualTo(123L)
            assertThat(result.content).isNotEmpty()
            assertThat(result.content).doesNotContain("```")
        }
    }

    @ParameterizedTest
    @DisplayName("잘못된 응답에 대해 실패 안내문 반환 테스트")
    @MethodSource("invalidResponseProvider")
    fun `should return failure notice for invalid responses`(response: String) {
        // given
        mockChatResponseWithText(response)

        // when
        val result: FakeNewsDto = processor.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(result.realNewsId).isEqualTo(123L)
            assertThat(result.content).contains("AI 생성에 실패하여 안내문으로 대체되었습니다")
            assertThat(result.content).contains("시스템 관리자에게 문의")
        }
    }

    @Test
    @DisplayName("null 응답에 대해 실패 안내문 반환 테스트")
    fun `should return failure notice for null response`() {
        // given
        every { mockChatResponse.result } returns null

        // when
        val result: FakeNewsDto = processor.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(result.realNewsId).isEqualTo(123L)
            assertThat(result.content).contains("AI 응답이 비어있습니다")
            assertThat(result.content).contains("안내문으로 대체되었습니다")
        }
    }

    @Test
    @DisplayName("content가 null인 응답 처리 테스트")
    fun `should handle null content in response`() {
        // given
        mockChatResponseWithText(NULL_CONTENT_RESPONSE)

        // when
        val result: FakeNewsDto = processor.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(result.realNewsId).isEqualTo(123L)
            assertThat(result.content).contains("content가 누락되었습니다")
        }
    }

    @Test
    @DisplayName("빈 content 응답 처리 테스트")
    fun `should handle empty content in response`() {
        // given
        mockChatResponseWithText(EMPTY_CONTENT_RESPONSE)

        // when
        val result: FakeNewsDto = processor.parseResponse(mockChatResponse)
        // then
        with(result) {
            assertThat(result.realNewsId).isEqualTo(123L)
            assertThat(result.content).contains("content가 누락되었습니다")
        }
    }

    @Test
    @DisplayName("buildPrompt 메소드 테스트")
    fun `should build prompt with real news data`() {
        // when
        val prompt = processor.buildPrompt()

        // then
        val expectedTexts = listOf(
            "삼성전자, AI 반도체 개발 성공으로 주가 급등",
            "당신은 가짜 뉴스 창작 전문가입니다",
            sampleRealNewsDto.content.length.toString(),
            "제목을 content에 포함하기",
            "JSON 형식이 정확한가?"
        )
        
        expectedTexts.forEach { expectedText ->
            assertThat(prompt).contains(expectedText)
        }
    }

    @Test
    @DisplayName("ObjectMapper 예외 발생시 실패 안내문 반환 테스트")
    fun `should return failure notice when ObjectMapper throws exception`() {
        // given
        val faultyObjectMapper = mockk<ObjectMapper> {
            every { readValue(any<String>(), any<Class<*>>()) } throws RuntimeException("JSON 파싱 실패")
        }

        val processorWithFaultyMapper = FakeNewsGeneratorProcessor(sampleRealNewsDto, faultyObjectMapper)
        mockChatResponseWithText(VALID_JSON_RESPONSE)

        // when
        val result: FakeNewsDto = processorWithFaultyMapper.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(result.realNewsId).isEqualTo(123L)
            assertThat(result.content).contains("AI 응답 파싱 실패")
        }
    }

    @Test
    @DisplayName("실제 ObjectMapper로 전체 플로우 테스트")
    fun `should work with real ObjectMapper end to end`() {
        // given
        val realProcessor = FakeNewsGeneratorProcessor(sampleRealNewsDto, ObjectMapper())
        mockChatResponseWithText(COMPLEX_JSON_RESPONSE)

        // when
        val result: FakeNewsDto = realProcessor.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(result.realNewsId).isEqualTo(123L)
            assertThat(result.content).isEqualTo("구글이 새로운 검색 알고리즘을 도입한다고 17일 발표했다. 이번 알고리즘은 사용자 경험을 크게 개선할 것으로 기대된다. 구글 엔지니어링 팀은 \"검색 정확도가 대폭 향상될 것\"이라고 설명했다. 업계에서는 이번 변화가 검색 시장에 큰 영향을 줄 것으로 예상한다고 밝혔다.")
            assertThat(result.content).doesNotContain("\\\\n") // 이스케이프 문자가 그대로 남아있지 않는지 확인
        }
    }

    @Test
    @DisplayName("특수문자가 포함된 content 처리 테스트")
    fun `should handle content with special characters`() {
        // given
        mockChatResponseWithText(SPECIAL_CHARACTERS_RESPONSE)

        // when
        val result: FakeNewsDto = processor.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(result.realNewsId).isEqualTo(123L)
            assertThat(result.content).contains("\"혁신적인 기술\"")
            assertThat(result.content).contains("50% 증가")
            assertThat(result.content).contains("AI\\\\n\\\\n새로운 시대")
        }
    }

    private fun mockChatResponseWithText(text: String) {
        val mockGeneration = mockk<Generation> {
            every { output } returns AssistantMessage(text)
        }
        every { mockChatResponse.result } returns mockGeneration
    }

    companion object {
        const val VALID_JSON_RESPONSE = """
            {
              "content": "Apple이 차세대 M3 프로세서를 발표했다고 16일 공식 발표했다. 새로운 프로세서는 이전 세대 대비 60% 빠른 처리 속도를 자랑하며, 배터리 효율성도 40% 개선됐다. Apple 대변인은 \"혁신적인 기술로 모바일 컴퓨팅의 새 지평을 열겠다\"고 밝혔다. 이에 따라 Apple 주식은 장중 6.8% 급등하며 180달러를 돌파했다."
            }
        """

        const val JSON_WITH_MARKDOWN = """
            ```json
            {
              "content": "마이크로소프트가 새로운 클라우드 서비스를 출시한다고 발표했다. 이 서비스는 기업용 데이터 처리 속도를 2배 향상시킬 것으로 예상된다. 마이크로소프트 CEO는 \"디지털 전환의 새로운 전환점이 될 것\"이라고 강조했다."
            }
            ```
        """

        const val NULL_CONTENT_RESPONSE = """
            {
              "content": null
            }
        """

        const val EMPTY_CONTENT_RESPONSE = """
            {
              "content": ""
            }
        """

        const val MALFORMED_JSON = """
            {
              "content": "테스트 내용이지만
            // JSON이 완성되지 않음
        """

        const val COMPLEX_JSON_RESPONSE = """
            {
              "content": "구글이 새로운 검색 알고리즘을 도입한다고 17일 발표했다. 이번 알고리즘은 사용자 경험을 크게 개선할 것으로 기대된다. 구글 엔지니어링 팀은 \"검색 정확도가 대폭 향상될 것\"이라고 설명했다. 업계에서는 이번 변화가 검색 시장에 큰 영향을 줄 것으로 예상한다고 밝혔다."
            }
        """

        const val SPECIAL_CHARACTERS_RESPONSE = """
            {
              "content": "테슬라가 \"혁신적인 기술\"로 배터리 성능을 50% 증가시켰다고 발표했다. 이로써 전기차 시장에서의 경쟁력이 더욱 강화될 전망이다. AI\\\\n\\\\n새로운 시대가 열릴 것으로 기대된다."
            }
        """

        @JvmStatic
        fun invalidResponseProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("빈 응답", ""),
            Arguments.of("공백만 있는 응답", "   \n\t   "),
            Arguments.of("잘못된 JSON 형식", MALFORMED_JSON),
            Arguments.of("JSON이 아닌 텍스트", "이것은 JSON이 아닙니다"),
            Arguments.of("불완전한 JSON", "{ \"content\":")
        )
    }
}