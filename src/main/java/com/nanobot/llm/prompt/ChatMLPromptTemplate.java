package com.nanobot.llm.prompt;

import com.nanobot.domain.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ChatML 格式的 Prompt 模板
 *
 * ChatML 格式示例：
 * <|im_start|>system
 * You are a helpful assistant.<|im_end|>
 * <|im_start|>user
 * Hello!<|im_end|>
 * <|im_start|>assistant
 * Hi! How can I help you?<|im_end|>
 *
 * 这种格式对小模型（如 BitNet）非常友好
 */
@Component("chatMLTemplate")
public class ChatMLPromptTemplate implements PromptTemplate {

    private static final String START_TOKEN = "<|im_start|>";
    private static final String END_TOKEN = "<|im_end|>";

    @Override
    public String format(List<Message> messages) {
        StringBuilder sb = new StringBuilder();

        for (Message message : messages) {
            String role = message.role();
            String content = getContent(message);

            if (content != null && !content.isEmpty()) {
                sb.append(START_TOKEN).append(role).append("\n");
                sb.append(content).append(END_TOKEN).append("\n");
            }
        }

        // 添加 assistant 开始标记，提示模型开始生成
        sb.append(START_TOKEN).append("assistant").append("\n");

        return sb.toString();
    }

    @Override
    public String getName() {
        return "ChatML";
    }

    /**
     * 提取消息内容
     */
    private String getContent(Message message) {
        if (message instanceof Message.UserMessage m) {
            return m.content();
        } else if (message instanceof Message.SystemMessage m) {
            return m.content();
        } else if (message instanceof Message.AssistantMessage m) {
            return formatAssistantMessage(m);
        } else if (message instanceof Message.ToolResultMessage m) {
            return formatToolResult(m);
        }
        throw new IllegalArgumentException("Unknown message type: " + message.getClass());
    }

    /**
     * 格式化 Assistant 消息
     * 如果有工具调用，需要特殊处理
     */
    private String formatAssistantMessage(Message.AssistantMessage message) {
        StringBuilder sb = new StringBuilder();

        if (message.content() != null && !message.content().isEmpty()) {
            sb.append(message.content());
        }

        // 如果有工具调用，添加到内容中
        if (message.hasToolCalls()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("[Calling tools: ");
            sb.append(message.toolCalls().stream()
                .map(tc -> tc.name())
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));
            sb.append("]");
        }

        return sb.toString();
    }

    /**
     * 格式化工具结果
     */
    private String formatToolResult(Message.ToolResultMessage message) {
        return String.format("[Tool: %s] Result: %s",
            message.toolName(),
            message.result());
    }
}
