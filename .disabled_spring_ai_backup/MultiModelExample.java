package com.nanobot.example;

import com.nanobot.domain.Message;
import com.nanobot.llm.annotation.UseModel;
import com.nanobot.llm.service.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 多模型使用示例
 *
 * 演示如何使用多模型架构：
 * 1. 使用默认模型
 * 2. 使用 @UseModel 注解指定模型
 * 3. 动态选择模型
 */
@Component
public class MultiModelExample implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MultiModelExample.class);

    private final LLMService llmService;

    public MultiModelExample(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Multi-Model Example ===");

        // 示例 1: 使用默认模型
        log.info("\n--- Example 1: Using Default Model ---");
        useDefaultModel();

        // 示例 2: 使用 @UseModel 注解
        log.info("\n--- Example 2: Using @UseModel Annotation ---");
        useDeepSeekModel();
        useKimiModel();

        // 示例 3: 动态选择模型
        log.info("\n--- Example 3: Dynamic Model Selection ---");
        useDynamicModel("deepseek");
        useDynamicModel("kimi");

        // 示例 4: 查看可用模型
        log.info("\n--- Example 4: Available Models ---");
        log.info("Available models: {}", llmService.getAvailableModels());
        log.info("Current model: {}", llmService.getCurrentModelName());
    }

    /**
     * 示例 1: 使用默认模型
     */
    private void useDefaultModel() {
        List<Message> messages = List.of(
                new Message.UserMessage("Hello! What's 2+2?")
        );

        Message.AssistantMessage response = llmService.chat(messages);
        log.info("Response: {}", response.content());
    }

    /**
     * 示例 2: 使用 DeepSeek 模型（通过注解）
     */
    @UseModel("deepseek")
    private void useDeepSeekModel() {
        List<Message> messages = List.of(
                new Message.UserMessage("Write a hello world in Java")
        );

        Message.AssistantMessage response = llmService.chat(messages);
        log.info("DeepSeek Response: {}", response.content());
    }

    /**
     * 示例 3: 使用 Kimi 模型（通过注解）
     */
    @UseModel("kimi")
    private void useKimiModel() {
        List<Message> messages = List.of(
                new Message.UserMessage("Explain what is Spring Boot in one sentence")
        );

        Message.AssistantMessage response = llmService.chat(messages);
        log.info("Kimi Response: {}", response.content());
    }

    /**
     * 示例 4: 动态选择模型
     */
    private void useDynamicModel(String modelName) {
        List<Message> messages = List.of(
                new Message.UserMessage("Say hello in Chinese")
        );

        Message.AssistantMessage response = llmService.chatWithModel(modelName, messages);
        log.info("{} Response: {}", modelName, response.content());
    }
}
