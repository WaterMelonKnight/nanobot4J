package com.nanobot.memory;

import com.nanobot.domain.Message;
import com.nanobot.llm.LLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 对话记忆摘要服务
 *
 * 核心功能：
 * 1. 异步生成对话摘要（使用 Java 21 虚拟线程）
 * 2. 调用 LLM 将历史对话压缩为简短摘要
 * 3. 将摘要存储到 Redis 中
 * 4. 支持配置摘要触发阈值和摘要长度
 */
@Service
public class MemorySummarizer {

    private static final Logger log = LoggerFactory.getLogger(MemorySummarizer.class);

    private static final String SUMMARY_PROMPT_TEMPLATE = """
            请将以下对话历史总结成一段简洁的摘要，不超过 100 字。

            要求：
            1. 提取关键信息和主要话题
            2. 保留重要的上下文信息
            3. 使用第三人称描述
            4. 简洁明了，去除冗余

            对话历史：
            %s

            请直接输出摘要内容，不要添加任何前缀或后缀。
            """;

    private final LLMClient llmClient;
    private final SummaryStore summaryStore;

    public MemorySummarizer(LLMClient llmClient, SummaryStore summaryStore) {
        this.llmClient = llmClient;
        this.summaryStore = summaryStore;
    }

    /**
     * 异步生成并保存对话摘要
     *
     * @param sessionId 会话ID
     * @param messages 需要摘要的消息列表
     * @return CompletableFuture，完成后返回摘要内容
     */
    public CompletableFuture<String> summarizeAsync(String sessionId, List<Message> messages) {
        log.info("Starting async summarization for session {}, {} messages", sessionId, messages.size());

        // 使用 ForkJoinPool 异步执行摘要任务（Java 17 兼容）
        return CompletableFuture.supplyAsync(() -> {
            try {
                return summarize(sessionId, messages);
            } catch (Exception e) {
                log.error("Failed to summarize session {}", sessionId, e);
                throw new RuntimeException("Summarization failed", e);
            }
        });
    }

    /**
     * 同步生成并保存对话摘要
     *
     * @param sessionId 会话ID
     * @param messages 需要摘要的消息列表
     * @return 摘要内容
     */
    public String summarize(String sessionId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("No messages to summarize for session {}", sessionId);
            return "";
        }

        log.debug("Summarizing {} messages for session {}", messages.size(), sessionId);

        try {
            // 1. 构建对话历史文本
            String conversationText = buildConversationText(messages);

            // 2. 构建摘要提示词
            String summaryPrompt = String.format(SUMMARY_PROMPT_TEMPLATE, conversationText);

            // 3. 调用 LLM 生成摘要
            List<Message> promptMessages = List.of(
                    new Message.SystemMessage("你是一个专业的对话摘要助手。"),
                    new Message.UserMessage(summaryPrompt)
            );

            Message.AssistantMessage response = llmClient.chat(promptMessages);
            String summary = response.content().trim();

            // 4. 保存摘要到 Redis
            summaryStore.saveSummary(sessionId, summary);

            log.info("Successfully summarized session {}: {} chars", sessionId, summary.length());
            return summary;

        } catch (Exception e) {
            log.error("Failed to generate summary for session {}", sessionId, e);
            throw new RuntimeException("Failed to generate summary", e);
        }
    }

    /**
     * 检查是否需要生成摘要
     *
     * @param messageCount 当前消息总数
     * @param windowSize 滑动窗口大小
     * @param hasSummary 是否已有摘要
     * @return 是否需要生成摘要
     */
    public boolean shouldSummarize(long messageCount, int windowSize, boolean hasSummary) {
        // 如果消息数超过窗口大小且还没有摘要，则需要生成
        return messageCount > windowSize && !hasSummary;
    }

    /**
     * 构建对话历史文本
     */
    private String buildConversationText(List<Message> messages) {
        StringBuilder sb = new StringBuilder();

        for (Message message : messages) {
            String role = message.role();
            String content = extractContent(message);

            if (content != null && !content.isEmpty()) {
                sb.append(role).append(": ").append(content).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 从消息中提取内容
     */
    private String extractContent(Message message) {
        if (message instanceof Message.UserMessage msg) {
            return msg.content();
        } else if (message instanceof Message.AssistantMessage msg) {
            return msg.content();
        } else if (message instanceof Message.SystemMessage msg) {
            return msg.content();
        } else if (message instanceof Message.ToolResultMessage msg) {
            return "使用工具: " + msg.toolName();
        }
        return "";
    }
}
