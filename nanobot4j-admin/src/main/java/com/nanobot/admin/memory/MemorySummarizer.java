package com.nanobot.admin.memory;

import com.nanobot.admin.service.LLMService;
import com.nanobot.core.llm.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 记忆摘要器 - 滑动窗口 + 异步压缩
 *
 * 核心机制：
 * 1. 滑动窗口：始终保留最近 10 条消息作为短期记忆
 * 2. 异步摘要：当消息超过 10 条时，在虚拟线程中生成前 N 条的摘要
 * 3. 摘要注入：将摘要作为 System Message 注入到上下文中
 *
 * 工作流程：
 * - 每次对话前调用 applyWindow() 获取窗口化的历史
 * - 如果消息数 > 10，返回：[摘要] + [最近10条]
 * - 如果消息数 <= 10，直接返回所有消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemorySummarizer {

    private final ChatMemoryStore chatMemoryStore;
    private final SummaryStore summaryStore;
    private final LLMService llmService;

    /**
     * 滑动窗口大小
     */
    private static final int WINDOW_SIZE = 10;

    /**
     * 触发摘要的阈值
     */
    private static final int SUMMARY_THRESHOLD = 10;

    /**
     * 应用滑动窗口，返回用于构建 Prompt 的消息列表
     *
     * @param sessionId 会话 ID
     * @return 窗口化的消息列表（可能包含摘要）
     */
    public List<Message> applyWindow(String sessionId) {
        long totalCount = chatMemoryStore.getMessageCount(sessionId);

        log.debug("Session {} has {} messages", sessionId, totalCount);

        // 情况1：消息数 <= 窗口大小，直接返回所有消息
        if (totalCount <= WINDOW_SIZE) {
            return chatMemoryStore.getMessages(sessionId);
        }

        // 情况2：消息数 > 窗口大小，需要应用滑动窗口
        List<Message> recentMessages = chatMemoryStore.getRecentMessages(sessionId, WINDOW_SIZE);

        // 检查是否已有摘要
        String existingSummary = summaryStore.getSummary(sessionId);

        if (existingSummary != null) {
            // 已有摘要，直接使用
            log.debug("Using existing summary for session {}", sessionId);
            return prependSummary(existingSummary, recentMessages);
        }

        // 没有摘要，返回最近的消息（摘要会在后台异步生成）
        log.debug("No summary found for session {}, returning recent messages only", sessionId);
        return recentMessages;
    }

    /**
     * 异步触发摘要生成（如果需要）
     *
     * 调用时机：每次添加新消息后
     *
     * @param sessionId 会话 ID
     */
    public void summarizeIfNeeded(String sessionId) {
        long totalCount = chatMemoryStore.getMessageCount(sessionId);

        // 只有当消息数刚好超过阈值且没有摘要时才触发
        if (totalCount > SUMMARY_THRESHOLD && !summaryStore.hasSummary(sessionId)) {
            log.info("Triggering async summarization for session {}", sessionId);

            // 使用 CompletableFuture 在虚拟线程中执行摘要
            CompletableFuture.runAsync(() -> {
                try {
                    generateAndSaveSummary(sessionId);
                } catch (Exception e) {
                    log.error("Failed to generate summary for session {}", sessionId, e);
                }
            });
        }
    }

    /**
     * 生成并保存摘要（在虚拟线程中执行）
     */
    private void generateAndSaveSummary(String sessionId) {
        log.info("Starting summary generation for session {}", sessionId);

        // 获取所有历史消息
        List<Message> allMessages = chatMemoryStore.getMessages(sessionId);

        if (allMessages.isEmpty()) {
            log.warn("No messages to summarize for session {}", sessionId);
            return;
        }

        // 计算需要摘要的消息数量（保留最近 WINDOW_SIZE 条，其余的进行摘要）
        int messagesToSummarize = Math.max(0, allMessages.size() - WINDOW_SIZE);

        if (messagesToSummarize == 0) {
            log.debug("Not enough messages to summarize for session {}", sessionId);
            return;
        }

        List<Message> oldMessages = allMessages.subList(0, messagesToSummarize);

        // 构建摘要 Prompt
        String summaryPrompt = buildSummaryPrompt(oldMessages);

        // 调用 LLM 生成摘要
        String summary = llmService.chat(
            "你是一个对话摘要助手。请用简洁的语言总结以下对话的关键信息，保留重要的上下文和决策。",
            summaryPrompt
        );

        // 保存摘要
        summaryStore.saveSummary(sessionId, summary);

        log.info("Summary generated and saved for session {}: {} chars", sessionId, summary.length());
    }

    /**
     * 构建摘要 Prompt
     */
    private String buildSummaryPrompt(List<Message> messages) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请总结以下对话内容：\n\n");

        for (Message msg : messages) {
            String roleLabel = getRoleLabel(msg.getRole());
            prompt.append(roleLabel).append(": ").append(msg.getContent()).append("\n");
        }

        prompt.append("\n请用 2-3 句话总结上述对话的核心内容和关键决策。");
        return prompt.toString();
    }

    /**
     * 获取角色标签
     */
    private String getRoleLabel(String role) {
        if (role == null) {
            return "Unknown";
        }

        switch (role) {
            case "user":
                return "用户";
            case "assistant":
                return "助手";
            case "system":
                return "系统";
            case "tool":
                return "工具";
            default:
                return role;
        }
    }

    /**
     * 将摘要添加到消息列表开头
     */
    private List<Message> prependSummary(String summary, List<Message> recentMessages) {
        Message summaryMessage = Message.system("【历史对话摘要】\n" + summary);

        // 创建新列表，摘要在前
        List<Message> result = new java.util.ArrayList<>();
        result.add(summaryMessage);
        result.addAll(recentMessages);

        return result;
    }

    /**
     * 手动触发摘要生成（用于测试或管理接口）
     *
     * @param sessionId 会话 ID
     * @return 生成的摘要内容
     */
    public String forceSummarize(String sessionId) {
        log.info("Force summarizing session {}", sessionId);

        List<Message> allMessages = chatMemoryStore.getMessages(sessionId);

        if (allMessages.isEmpty()) {
            return "No messages to summarize";
        }

        String summaryPrompt = buildSummaryPrompt(allMessages);

        String summary = llmService.chat(
            "你是一个对话摘要助手。请用简洁的语言总结以下对话的关键信息。",
            summaryPrompt
        );

        summaryStore.saveSummary(sessionId, summary);

        return summary;
    }
}
