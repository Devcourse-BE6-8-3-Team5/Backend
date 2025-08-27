package com.back.global.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI의 ChatClient를 빈으로 등록하는 설정 클래스입니다.
 * ChatModel은 application.yml 설정을 기반으로 생성됩니다.
 */
@Configuration
public class AiConfig {
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
