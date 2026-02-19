package com.nanobot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * LLM 客户端配置
 *
 * 配置多个 LLM 客户端：
 * 1. Ollama（本地模型）
 * 2. OpenAI（云端模型）
 * 3. Fallback（降级策略）
 */
@Configuration
public class LLMClientConfig {

    @Value("${nanobot.llm.primary:ollama}")
    private String primaryLLM;

    /**
     * Ollama ChatClient Builder
     */
    @Bean("ollamaChatClientBuilder")
    public ChatClient.Builder ollamaChatClientBuilder(ChatClient.Builder builder) {
        return builder;
    }

    /**
     * OpenAI ChatClient Builder
     */
    @Bean("openaiChatClientBuilder")
    public ChatClient.Builder openaiChatClientBuilder(ChatClient.Builder builder) {
        return builder;
    }

    /**
     * 默认使用 Fallback 客户端
     */
    @Bean
    @Primary
    public String primaryLLMType() {
        return primaryLLM;
    }
}
