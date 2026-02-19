package com.nanobot.llm.ollama;

import com.nanobot.domain.Message;
import com.nanobot.domain.ToolCall;
import com.nanobot.llm.LLMClient;
import com.nanobot.llm.prompt.PromptTemplate;
import com.nanobot.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ollama LLM 客户端
 *
 * 特点：
 * 1. 使用 PromptTemplate 格式化消息（适配小模型）
 * 2. 不支持原生 Function Calling（小模型通常不支持）
 * 3. 超时时间较短（本地模型响应快）
 */
@Component("ollamaLLMClient")
public class OllamaLLMClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaLLMClient.class);

    private final ChatClient chatClient;
    private final PromptTemplate promptTemplate;
    private final String modelName;

    public OllamaLLMClient(
            @Qualifier("ollamaChatClientBuilder") ChatClient.Builder chatClientBuilder,
            @Qualifier("chatMLTemplate") PromptTemplate promptTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate;
        this.modelName = "bitnet"; // 可以从配置中读取
    }

    @Override
    public Message.AssistantMessage chat(List<Message> messages) {
        return chatWithTools(messages, List.of());
    }

    @Override
    public Message.AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools) {
        try {
            // 使用 PromptTemplate 格式化消息
            String formattedPrompt = promptTemplate.format(messages);

            log.debug("Formatted prompt for Ollama:\n{}", formattedPrompt);

            // 调用 Ollama
            Prompt prompt = new Prompt(formattedPrompt);
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

            String content = response.getResult().getOutput().getContent();

            // Ollama 不支持原生 Function Calling，返回纯文本
            return new Message.AssistantMessage(content, List.of());

        } catch (Exception e) {
            log.error("Ollama chat failed", e);
            throw new RuntimeException("Ollama chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
