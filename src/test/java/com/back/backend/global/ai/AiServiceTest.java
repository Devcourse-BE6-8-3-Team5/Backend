package com.back.backend.global.ai;

import com.back.domain.quiz.detail.dto.DetailQuizCreateReqDto;
import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.entity.Option;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.DetailQuizProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AiServiceTest {
    @Test
    @DisplayName("process(with DetailQuizProcessor)는 파싱된 결과값을 반환해야 한다")
    void t1() {

        // given
        ChatClient mockChatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new KotlinModule.Builder().build());

        // Mock ChatResponse
        ChatResponse mockResponse = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(mockResponse.getResult().getOutput().getText()).thenReturn("""
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
        """);

        when(mockChatClient.prompt(anyString()).call().chatResponse()).thenReturn(mockResponse);

        AiService aiService = new AiService(mockChatClient);

        DetailQuizProcessor processor = new DetailQuizProcessor(
                new DetailQuizCreateReqDto("제목", "본문"),
                objectMapper
        );

        // when
        List<DetailQuizDto> result = (List<DetailQuizDto>) aiService.process(processor);

        // then
        System.out.println(result);
        assertThat(result).hasSize(3);

        // 첫 번째 퀴즈 검증
        DetailQuizDto firstQuiz = result.get(0);
        assertThat(firstQuiz.getQuestion()).isEqualTo("알리바바가 발표한 모델 이름은?");
        assertThat(firstQuiz.getOption1()).isEqualTo("Qwen3-Coder");
        assertThat(firstQuiz.getOption2()).isEqualTo("GPT-4");
        assertThat(firstQuiz.getOption3()).isEqualTo("Claude 4");
        assertThat(firstQuiz.getCorrectOption()).isEqualTo(Option.OPTION1);

        // 두 번째, 세 번째 퀴즈 정답값 확인 (다양성 검증)
        assertThat(result.get(1).getCorrectOption()).isEqualTo(Option.OPTION3);
        assertThat(result.get(2).getCorrectOption()).isEqualTo(Option.OPTION1);
    }
}

