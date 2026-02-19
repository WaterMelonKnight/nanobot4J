package com.nanobot.llm.prompt;

import com.nanobot.domain.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Alpaca 格式的 Prompt 模板
 *
 * Alpaca 格式示例：
 * Below is an instruction that describes a task. Write a response that appropriately completes the request.
 *
 * ### Instruction:
 * You are a helpful assistant.
 *
 * ### Input:
 * Hello!
 *
 * ### Response:
 * Hi! How can I help you?
 *
 * 适用于基于 LLaMA 的模型
 */
@Component("alpacaTemplate")
public class AlpacaPromptTemplate implements PromptTemplate {

    private static final String HEADER = "Below is an instruction that describes a task. " +
        "Write a response that appropriately completes the request.\n\n";

    @Override
    public String format(List<Message> messages) {
        StringBuilder sb = new StringBuilder(HEADER);

        // 提取系统消息作为 Instruction
        String systemPrompt = messages.stream()
            .filter(m -> m instanceof Message.SystemMessage)
            .map(m -> ((Message.SystemMessage) m).content())
            .findFirst()
            .orElse("You are a helpful assistant.");

        sb.append("### Instruction:\n");
        sb.append(systemPrompt).append("\n\n");

        // 处理对话历史
        List<Message> conversationMessages = messages.stream()
            .filter(m -> !(m instanceof Message.SystemMessage))
            .toList();

        for (int i = 0; i < conversationMessages.size(); i++) {
            Message message = conversationMessages.get(i);

            if (message instanceof Message.UserMessage m) {
                sb.append("### Input:\n");
                sb.append(m.content()).append("\n\n");
            } else if (message instanceof Message.AssistantMessage m) {
                sb.append("### Response:\n");
                sb.append(m.content()).append("\n\n");
            } else if (message instanceof Message.ToolResultMessage m) {
                sb.append("### Tool Result:\n");
                sb.append(String.format("[%s] %s", m.toolName(), m.result())).append("\n\n");
            }
        }

        // 添加 Response 开始标记
        sb.append("### Response:\n");

        return sb.toString();
    }

    @Override
    public String getName() {
        return "Alpaca";
    }
}
