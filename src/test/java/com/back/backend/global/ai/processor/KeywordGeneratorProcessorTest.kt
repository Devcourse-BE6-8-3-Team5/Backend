package com.back.backend.global.ai.processor


import com.back.domain.news.common.dto.KeywordGenerationReqDto
import com.back.domain.news.common.dto.KeywordGenerationResDto
import com.back.domain.news.common.enums.KeywordType
import com.back.global.ai.processor.KeywordGeneratorProcessor
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
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import java.time.LocalDate
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeywordGeneratorProcessorTest {

    private lateinit var processor: KeywordGeneratorProcessor
    private lateinit var objectMapper: ObjectMapper
    private lateinit var mockChatResponse: ChatResponse
    private lateinit var keywordGenerationReqDto: KeywordGenerationReqDto

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        keywordGenerationReqDto = KeywordGenerationReqDto(
            currentDate = LocalDate.of(2025, 8, 29),
            recentKeywordsWithTypes = listOf("AI(BREAKING), 부동산(ONGOING)"),
            excludeKeywords = listOf("폭염", "K팝")
        )
        processor = KeywordGeneratorProcessor(keywordGenerationReqDto, objectMapper)
        mockChatResponse = mockk<ChatResponse>()
    }

    @Test
    @DisplayName("정상적인 AI 응답 파싱 성공 테스트")
    fun t1() {
        // given
        mockChatResponseWithText(VALID_JSON_RESPONSE)
        val result = processor.parseResponse(mockChatResponse)

        // then
        assertThat(result.society).hasSize(2)
        assertThat(result.economy).hasSize(2)
        assertThat(result.politics).hasSize(2)
        assertThat(result.culture).hasSize(2)
        assertThat(result.it).hasSize(2)

        assertThat(result.society[0].keyword).isEqualTo("교육")
        assertThat(result.society[0].keywordType).isEqualTo(KeywordType.GENERAL)
        assertThat(result.economy[0].keyword).isEqualTo("금리")
        assertThat(result.economy[0].keywordType).isEqualTo(KeywordType.BREAKING)
    }

    @Test
    @DisplayName("JSON 마크다운 없는 순수 JSON 응답 파싱 테스트")
    fun t2() {
        // given
        mockChatResponseWithText(PURE_JSON_RESPONSE)

        // when
        val result = processor.parseResponse(mockChatResponse)

        // then
        assertThat(result.society[0].keyword).isEqualTo("사회")
        assertThat(result.economy[1].keyword).isEqualTo("투자")
    }

    
    @ParameterizedTest
    @DisplayName("잘못된 응답에 대해 기본 키워드 반환 테스트")
    @MethodSource("malformedResponseProvider")
    fun t3(response: String) {
        // given
        mockChatResponseWithText(response)
        
        // when
        val result = processor.parseResponse(mockChatResponse)
        
        // then
        with(result) {
            assertThat(society).hasSize(2)
            assertThat(society[0].keyword).isEqualTo("사회")
            assertThat(society[1].keyword).isEqualTo("교육")
            assertThat(economy[0].keyword).isEqualTo("경제")
            assertThat(economy[1].keyword).isEqualTo("시장")
            assertThat(politics[0].keyword).isEqualTo("정치")
            assertThat(politics[1].keyword).isEqualTo("정부")
            assertThat(culture[0].keyword).isEqualTo("문화")
            assertThat(culture[1].keyword).isEqualTo("예술")
            assertThat(it[0].keyword).isEqualTo("기술")
            assertThat(it[1].keyword).isEqualTo("IT")

            // 모든 키워드가 GENERAL 타입인지 확인
            listOf(society, economy, politics, culture, it).forEach { category ->
                category.forEach { keyword ->
                    println(keyword)
                    assertThat(keyword.keywordType).isEqualTo(KeywordType.GENERAL)
                }
            }
        }
    }

    @ParameterizedTest
    @DisplayName("다양한 KeywordType 파싱 테스트")
    @EnumSource(KeywordType::class)
    fun t4(keywordType: KeywordType) {
        // given
        mockChatResponseWithText(responseWithKeywordType(keywordType))

        // when
        val result = processor.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(society[0].keywordType).isEqualTo(keywordType)
            assertThat(economy[0].keywordType).isEqualTo(keywordType)
            assertThat(politics[0].keywordType).isEqualTo(keywordType)
            assertThat(culture[0].keywordType).isEqualTo(keywordType)
            assertThat(it[0].keywordType).isEqualTo(keywordType)
        }
    }

    @Test
    @DisplayName("buildPrompt 메소드 테스트")
    fun t5() {
        // when
        val prompt = processor.buildPrompt()

        // then
        val expectedTexts = listOf(
            "2025-08-29",
            "AI(BREAKING), 부동산(ONGOING)",
            "폭염",
            "K팝",
            "Task: 오늘 뉴스 수집을 위한"
        )
        
        expectedTexts.forEach { expectedText ->
            assertThat(prompt).contains(expectedText)
        }
    }

    @Test
    @DisplayName("ObjectMapper 예외 발생시 기본 키워드 반환 테스트")
    fun t6() {
        // given
        val faultyObjectMapper = mockk<ObjectMapper> {
            every { readValue(any<String>(), KeywordGenerationResDto::class.java) } throws RuntimeException("JSON 파싱 실패")
        }

        val processorWithFaultyMapper = KeywordGeneratorProcessor(keywordGenerationReqDto, faultyObjectMapper)
        mockChatResponseWithText(VALID_JSON_RESPONSE)

        // when
        val result = processorWithFaultyMapper.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(society[0].keyword).isEqualTo("사회")
            assertThat(economy[0].keyword).isEqualTo("경제")
            assertThat(politics[0].keyword).isEqualTo("정치")
            assertThat(culture[0].keyword).isEqualTo("문화")
            assertThat(it[0].keyword).isEqualTo("기술")
        }
    }

    @Test
    @DisplayName("실제 ObjectMapper로 전체 플로우 테스트")
    fun t7() {
        // given
        val realProcessor = KeywordGeneratorProcessor(keywordGenerationReqDto, ObjectMapper())
        mockChatResponseWithText(END_TO_END_RESPONSE)

        // when
        val result = realProcessor.parseResponse(mockChatResponse)

        // then
        with(result) {
            assertThat(society[0].keyword).isEqualTo("시험")
            assertThat(society[0].keywordType).isEqualTo(KeywordType.SEASONAL)
            assertThat(economy[0].keyword).isEqualTo("환율")
            assertThat(economy[0].keywordType).isEqualTo(KeywordType.BREAKING)
        }
    }

    private fun mockChatResponseWithText(text: String) {
        val mockGeneration = mockk<Generation> {
            every { output } returns AssistantMessage(text)
        }
        every { mockChatResponse.result } returns mockGeneration
    }

    fun malformedResponseProvider(): Stream<Arguments> = Stream.of(
        Arguments.of("빈 응답", ""),
        Arguments.of("공백만 있는 응답", "   \n\t   "),
        Arguments.of("잘못된 JSON 형식", MALFORMED_JSON),
        Arguments.of("일부 카테고리 누락", incompleteCategoriesJson())
    )

    companion object {

        const val VALID_JSON_RESPONSE = """
            {
              "society": [
                {"keyword": "교육", "keywordType": "GENERAL"},
                {"keyword": "안전", "keywordType": "ONGOING"}
              ],
              "economy": [
                {"keyword": "금리", "keywordType": "BREAKING"},
                {"keyword": "부동산", "keywordType": "ONGOING"}
              ],
              "politics": [
                {"keyword": "국회", "keywordType": "ONGOING"},
                {"keyword": "정책", "keywordType": "GENERAL"}
              ],
              "culture": [
                {"keyword": "영화", "keywordType": "GENERAL"},
                {"keyword": "축제", "keywordType": "SEASONAL"}
              ],
              "it": [
                {"keyword": "AI", "keywordType": "BREAKING"},
                {"keyword": "반도체", "keywordType": "ONGOING"}
              ]
            }
        """

        const val PURE_JSON_RESPONSE = """
            {
              "society": [
                {"keyword": "사회", "keywordType": "GENERAL"},
                {"keyword": "복지", "keywordType": "ONGOING"}
              ],
              "economy": [
                {"keyword": "경제", "keywordType": "GENERAL"},
                {"keyword": "투자", "keywordType": "BREAKING"}
              ],
              "politics": [
                {"keyword": "정치", "keywordType": "GENERAL"},
                {"keyword": "의회", "keywordType": "ONGOING"}
              ],
              "culture": [
                {"keyword": "문화", "keywordType": "GENERAL"},
                {"keyword": "공연", "keywordType": "SEASONAL"}
              ],
              "it": [
                {"keyword": "기술", "keywordType": "GENERAL"},
                {"keyword": "소프트웨어", "keywordType": "ONGOING"}
              ]
            }
        """

        const val MALFORMED_JSON = """
            {
              "society": [
                {"keyword": "교육", "keywordType": "GENERAL"
              // 잘못된 JSON - 닫는 괄호 누락
        """

        const val END_TO_END_RESPONSE = """
            {
              "society": [{"keyword": "시험", "keywordType": "SEASONAL"}, {"keyword": "복지", "keywordType": "ONGOING"}],
              "economy": [{"keyword": "환율", "keywordType": "BREAKING"}, {"keyword": "증시", "keywordType": "GENERAL"}],
              "politics": [{"keyword": "개헌", "keywordType": "ONGOING"}, {"keyword": "외교", "keywordType": "GENERAL"}],
              "culture": [{"keyword": "올림픽", "keywordType": "SEASONAL"}, {"keyword": "드라마", "keywordType": "ONGOING"}],
              "it": [{"keyword": "메타버스", "keywordType": "BREAKING"}, {"keyword": "클라우드", "keywordType": "GENERAL"}]
            }
        """

        // 동적 생성 메서드들
        private fun responseWithKeywordType(keywordType: KeywordType) = """
            {
              "society": [
                {"keyword": "교육", "keywordType": "${keywordType.name}"},
                {"keyword": "안전", "keywordType": "GENERAL"}
              ],
              "economy": [
                {"keyword": "금리", "keywordType": "${keywordType.name}"},
                {"keyword": "부동산", "keywordType": "GENERAL"}
              ],
              "politics": [
                {"keyword": "국회", "keywordType": "${keywordType.name}"},
                {"keyword": "정책", "keywordType": "GENERAL"}
              ],
              "culture": [
                {"keyword": "영화", "keywordType": "${keywordType.name}"},
                {"keyword": "축제", "keywordType": "GENERAL"}
              ],
              "it": [
                {"keyword": "AI", "keywordType": "${keywordType.name}"},
                {"keyword": "반도체", "keywordType": "GENERAL"}
              ]
            }
        """.trimIndent()

        private fun incompleteCategoriesJson() = """
            {
              "society": [
                {"keyword": "교육", "keywordType": "GENERAL"},랴ㅜ
                {"keyword": "안전", "keywordType": "ONGOING"}
              ],
              "economy": [
                {"keyword": "경제", "keywordType": "GENERAL"},
                {"keyword": "시장", "keywordType": "ONGOING"}
              ]
            }
        """.trimIndent()

    }
}