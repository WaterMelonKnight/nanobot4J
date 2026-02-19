package com.nanobot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置
 */
@Configuration
public class SpringAIConfig {

    @Bean
    public ChatClient.Builder chatClientBuilder(ChatClient.Builder builder) {
        return builder;
    }
}
