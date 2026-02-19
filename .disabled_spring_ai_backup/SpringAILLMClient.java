package com.nanobot.llm;

import com.nanobot.domain.Message;
import com.nanobot.domain.ToolCall;
import com.nanobot.tool.Tool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 Spring AI 的 LLM 客户端实现
 *
 * 设计思路：
 * 1. 使用 Spring AI 的 ChatClient 进行底层调用
 * 2. 将我们的 Message 模型转换为 Spring AI 的消息格式
 * 3. 处理工具调用的序列化和反序列化
 */
@Component
public class SpringAILLMClient implements LLMClient {

    private final ChatClient chatClient;
    private final String modelName;

    public SpringAILLMClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.modelName = "gpt-4"; // 可以从配置中读取
    }

    @Override
    public Message.AssistantMessage chat(List<Message> messages) {
        return chatWithTools(messages, List.of());
    }

    @Override
    public Message.AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools) {
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

        // 调用 LLM
        ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

        // 转换响应
        return convertToAssistantMessage(response);
    }

    @Override
    public String getModelName() {
        return modelName;
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
        // Spring AI 需要函数名称列表
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
        var metadata = result.getMetadata();

        // 这里需要根据实际的 Spring AI API 来提取工具调用
        // 简化版本：假设没有工具调用

        return new Message.AssistantMessage(content, toolCalls);
    }
}
