package com.nanobot.llm.mock;

import com.nanobot.domain.Message;
import com.nanobot.llm.LLMClient;
import com.nanobot.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Mock LLM Client - 用于演示和测试
 *
 * 这是一个简单的模拟实现，返回预定义的响应。
 * 在生产环境中，应该使用真实的 LLM 客户端（如 OpenAI、Ollama 等）。
 */
@Component
public class MockLLMClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(MockLLMClient.class);

    @Override
    public Message.AssistantMessage chat(List<Message> messages) {
        return chatWithTools(messages, List.of());
    }

    @Override
    public String getModelName() {
        return "mock-llm-client";
    }

    @Override
    public Message.AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools) {
        log.info("MockLLMClient received {} messages", messages.size());

        // 获取最后一条用户消息
        String userMessage = messages.stream()
                .filter(m -> m instanceof Message.UserMessage)
                .map(m -> ((Message.UserMessage) m).content())
                .reduce((first, second) -> second)
                .orElse("");

        // 生成简单的响应
        String response = generateMockResponse(userMessage);

        return new Message.AssistantMessage(
                "mock-" + System.currentTimeMillis(),
                response,
                List.of(),
                Instant.now()
        );
    }

    private String generateMockResponse(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();

        if (lowerMessage.contains("你好") || lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "你好！我是 Nanobot4J 的 Mock LLM 客户端。目前使用的是模拟响应。\n\n" +
                   "要使用真实的 LLM 模型，请配置以下选项之一：\n" +
                   "1. OpenAI API\n" +
                   "2. Ollama 本地模型\n" +
                   "3. DeepSeek API\n" +
                   "4. 其他兼容 OpenAI 的 API";
        }

        if (lowerMessage.contains("帮助") || lowerMessage.contains("help")) {
            return "Nanobot4J 功能说明：\n\n" +
                   "✓ 多轮对话管理\n" +
                   "✓ 会话持久化\n" +
                   "✓ Agent 配置管理\n" +
                   "✓ 工具调用支持\n" +
                   "✓ RESTful API\n" +
                   "✓ Swagger 文档\n\n" +
                   "当前使用 Mock 模式，请配置真实的 LLM 客户端以获得完整功能。";
        }

        if (lowerMessage.contains("计算") || lowerMessage.contains("数学")) {
            return "我是一个 Mock 客户端，无法执行真实的计算。\n" +
                   "配置真实的 LLM 客户端后，我可以帮你进行数学计算和其他任务。";
        }

        // 默认响应
        return String.format("收到你的消息：「%s」\n\n" +
                           "这是来自 Mock LLM 客户端的响应。当前系统运行正常，但使用的是模拟模式。\n\n" +
                           "💡 提示：要获得真实的 AI 对话能力，请在 application.yml 中配置 LLM 服务。",
                           userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage);
    }
}
