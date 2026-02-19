package com.nanobot.llm.openai;

import com.nanobot.domain.Message;
import com.nanobot.domain.ToolCall;
import com.nanobot.llm.LLMClient;
import com.nanobot.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI 兼容的 LLM 客户端
 *
 * 支持所有 OpenAI API 兼容的提供商：
 * - DeepSeek
 * - Kimi (Moonshot)
 * - OpenAI
 * - 其他兼容 OpenAI API 的服务
 *
 * 特点：
 * 1. 支持原生 Function Calling
 * 2. 使用标准的 OpenAI 消息格式
 * 3. 支持流式和非流式响应
 */
public class OpenAICompatibleLLMClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAICompatibleLLMClient.class);

    private final ChatClient chatClient;
    private final String providerName;
    private final String modelName;

    public OpenAICompatibleLLMClient(ChatClient chatClient, String providerName, String modelName) {
        this.chatClient = chatClient;
        this.providerName = providerName;
        this.modelName = modelName;
    }

    @Override
    public Message.AssistantMessage chat(List<Message> messages) {
        return chatWithTools(messages, List.of());
    }

    @Override
    public Message.AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools) {
        try {
            // 转换消息格式
            List<org.springframework.ai.chat.messages.Message> springMessages =
                    convertToSpringMessages(messages);

            // 构建 Prompt
            Prompt prompt;
            if (tools.isEmpty()) {
                prompt = new Prompt(springMessages);
            } else {
                // 添加工具定义
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .withFunctions(convertToolsToFunctions(tools))
                        .build();
                prompt = new Prompt(springMessages, options);
            }

            log.debug("[{}] Sending request to model: {}", providerName, modelName);

            // 调用 LLM
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

            // 转换响应
            return convertToAssistantMessage(response);

        } catch (Exception e) {
            log.error("[{}] Chat failed", providerName, e);
            throw new RuntimeException(providerName + " chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getModelName() {
        return providerName + ":" + modelName;
    }

    /**
     * 将我们的 Message 转换为 Spring AI 的消息格式
     */
    private List<org.springframework.ai.chat.messages.Message> convertToSpringMessages(
            List<Message> messages) {
        return messages.stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());
    }

    private org.springframework.ai.chat.messages.Message convertMessage(Message message) {
        if (message instanceof Message.UserMessage m) {
            return new UserMessage(m.content());
        } else if (message instanceof Message.AssistantMessage m) {
            return new AssistantMessage(m.content());
        } else if (message instanceof Message.SystemMessage m) {
            return new SystemMessage(m.content());
        } else if (message instanceof Message.ToolResultMessage m) {
            return new ToolResponseMessage(m.result(), m.toolCallId());
        }
        throw new IllegalArgumentException("Unknown message type: " + message.getClass());
    }

    /**
     * 将 Tool 转换为 Spring AI 的 Function 定义
     */
    private List<String> convertToolsToFunctions(List<Tool> tools) {
        return tools.stream()
                .map(Tool::getName)
                .collect(Collectors.toList());
    }

    /**
     * 将 Spring AI 的响应转换为我们的 AssistantMessage
     */
    private Message.AssistantMessage convertToAssistantMessage(ChatResponse response) {
        var result = response.getResult();
        String content = result.getOutput().getContent();

        // 提取工具调用
        List<ToolCall> toolCalls = new ArrayList<>();
        // TODO: 实现工具调用的提取逻辑

        return new Message.AssistantMessage(content, toolCalls);
    }
}
