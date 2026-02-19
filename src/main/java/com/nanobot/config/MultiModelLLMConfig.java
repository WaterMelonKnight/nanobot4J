package com.nanobot.config;

import com.nanobot.llm.LLMClient;
import com.nanobot.llm.http.SimpleHttpLLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 多模型 LLM 配置
 */
@Configuration
public class MultiModelLLMConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiModelLLMConfig.class);

    private final MultiModelProperties properties;

    public MultiModelLLMConfig(MultiModelProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    public LLMClient llmClient() {
        String defaultModel = properties.getDefaultModel();
        MultiModelProperties.ModelConfig config = properties.getModels().get(defaultModel);

        if (config == null || !config.isEnabled()) {
            log.warn("Default model '{}' not found or disabled, trying fallback models", defaultModel);

            // 尝试使用 fallback 顺序中的第一个可用模型
            for (String modelName : properties.getFallback().getOrder()) {
                MultiModelProperties.ModelConfig fallbackConfig = properties.getModels().get(modelName);
                if (fallbackConfig != null && fallbackConfig.isEnabled()) {
                    log.info("Using fallback model: {}", modelName);
                    return createClient(modelName, fallbackConfig);
                }
            }

            // 如果都不可用，尝试找任何一个启用的模型
            for (var entry : properties.getModels().entrySet()) {
                if (entry.getValue().isEnabled()) {
                    log.info("Using available model: {}", entry.getKey());
                    return createClient(entry.getKey(), entry.getValue());
                }
            }

            throw new IllegalStateException("No enabled LLM model found in configuration");
        }

        log.info("Initializing default LLM client: {} ({})", defaultModel, config.getModel());
        return createClient(defaultModel, config);
    }

    private LLMClient createClient(String modelName, MultiModelProperties.ModelConfig config) {
        if ("openai-compatible".equals(config.getProvider())) {
            log.info("Creating OpenAI-compatible client for {}: {}", modelName, config.getBaseUrl());
            return new SimpleHttpLLMClient(modelName, config);
        } else if ("ollama".equals(config.getProvider())) {
            log.warn("Ollama provider not yet implemented, using HTTP client as fallback");
            return new SimpleHttpLLMClient(modelName, config);
        } else {
            throw new IllegalArgumentException("Unsupported provider: " + config.getProvider());
        }
    }
}
