package com.nanobot.config;

import com.nanobot.llm.factory.ModelProviderFactory;
import com.nanobot.llm.prompt.ChatMLPromptTemplate;
import com.nanobot.llm.prompt.PromptTemplate;
import com.nanobot.llm.service.LLMService;
import com.nanobot.llm.service.MultiModelLLMService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 多模型配置类
 *
 * 职责：
 * 1. 启用 MultiModelProperties 配置
 * 2. 创建 ModelProviderFactory Bean
 * 3. 创建 LLMService Bean
 * 4. 启用 AOP 支持（用于 @UseModel 注解）
 */
@Configuration
@EnableConfigurationProperties(MultiModelProperties.class)
@EnableAspectJAutoProxy
public class MultiModelConfig {

    /**
     * ChatML Prompt 模板（用于 Ollama 等小模型）
     */
    @Bean("chatMLTemplate")
    public PromptTemplate chatMLTemplate() {
        return new ChatMLPromptTemplate();
    }

    /**
     * 模型提供商工厂
     */
    @Bean
    public ModelProviderFactory modelProviderFactory(
            MultiModelProperties properties,
            PromptTemplate chatMLTemplate) {
        return new ModelProviderFactory(properties, chatMLTemplate);
    }

    /**
     * LLM 服务（主要的业务接口）
     */
    @Bean
    public LLMService llmService(
            ModelProviderFactory factory,
            MultiModelProperties properties) {
        return new MultiModelLLMService(factory, properties);
    }
}
