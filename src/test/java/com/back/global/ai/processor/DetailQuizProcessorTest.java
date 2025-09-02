package com.back.global.ai.processor;

import com.back.domain.quiz.detail.dto.DetailQuizCreateReqDto;
import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.entity.Option;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DetailQuizProcessorTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new KotlinModule.Builder().build());
    private DetailQuizCreateReqDto defaultRequest;

    @BeforeEach
    void setUp() {
        defaultRequest = new DetailQuizCreateReqDto("테스트 뉴스 제목", "테스트 뉴스 본문 내용입니다.");
    }

    @Test
    @DisplayName("buildPrompt는 req의 title과 content를 포함해야 한다.")
    void t1() {
        // given
        DetailQuizProcessor processor = new DetailQuizProcessor(defaultRequest, objectMapper);

        // when
        String prompt = processor.buildPrompt();

        // then
        assertThat(prompt).contains("테스트 뉴스 제목");
        assertThat(prompt).contains("테스트 뉴스 본문 내용입니다.");
        assertThat(prompt).contains("객관식 퀴즈 3개");
    }

    @Test
    @DisplayName("parseResponse는 3개의 DetailQuizDto를 반환해야 한다.")
    void t2() {
        // given
        DetailQuizProcessor processor = new DetailQuizProcessor(defaultRequest, objectMapper);

        String mockJson = """
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
        """;

        // ChatResponse mock 생성
        ChatResponse mockResponse = createMockChatResponse(mockJson);

        // when
        List<DetailQuizDto> result = processor.parseResponse(mockResponse);
        DetailQuizDto first = result.get(0);

        // then
        assertThat(result).hasSize(3);

        assertThat(first.getQuestion()).isEqualTo("Qwen3-Coder는 어떤 회사가 공개했나요?");
        assertThat(first.getOption1()).isEqualTo("알리바바");
        assertThat(first.getOption2()).isEqualTo("구글");
        assertThat(first.getOption3()).isEqualTo("메타");
        assertThat(first.getCorrectOption()).isEqualTo(Option.OPTION1);

        assertThat(result.get(1).getCorrectOption()).isEqualTo(Option.OPTION2);
    }

    @Test
    @DisplayName("parseResponse는 null 응답에 대해 예외를 던져야 한다.")
    void t3() {
        // given
        DetailQuizProcessor processor = new DetailQuizProcessor(defaultRequest, objectMapper);
        ChatResponse mockResponse = createMockChatResponse(null);

        // when & then
        assertThatThrownBy(() -> processor.parseResponse(mockResponse))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("AI 응답이 비어있습니다");
    }

    @Test
    @DisplayName("parseResponse는 응답 결과가 json 형식이 아니면 예외를 던져야 한다.")
    void t4() {
        // given
        DetailQuizProcessor processor = new DetailQuizProcessor(defaultRequest, objectMapper);

        ChatResponse mockResponse = createMockChatResponse("INVALID_JSON");

        // when & then
        assertThatThrownBy(() -> processor.parseResponse(mockResponse))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("JSON 형식이 아닙니다");
    }

    @Test
    @DisplayName("parseResponse는 응답 결과 생성된 퀴즈가 3개가 아니면 예외를 던져야 한다.")
    void t5() {
        // given
        DetailQuizProcessor processor = new DetailQuizProcessor(defaultRequest, objectMapper);

        String mockJson = """
            [
              {
                "question": "하나만 있는 퀴즈",
                "option1": "정답",
                "option2": "오답1",
                "option3": "오답2",
                "correctOption": "OPTION1"
              }
            ]
        """;

        ChatResponse mockResponse = createMockChatResponse(mockJson);

        // when & then
        assertThatThrownBy(() -> processor.parseResponse(mockResponse))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("3개의 퀴즈");
    }

    /**
     * ChatResponse 목 객체를 생성하는 헬퍼 메서드 - 명시적 모킹
     */
    private ChatResponse createMockChatResponse(String responseText) {
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        AssistantMessage mockOutput = mock(AssistantMessage.class);

        // 체인 호출을 단계별로 설정
        when(mockResponse.getResult()).thenReturn(mockGeneration);
        when(mockGeneration.getOutput()).thenReturn(mockOutput);
        when(mockOutput.getText()).thenReturn(responseText);

        return mockResponse;
    }

}

