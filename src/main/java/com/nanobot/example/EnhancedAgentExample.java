package com.nanobot.example;

import com.nanobot.agent.EnhancedAgent;
import com.nanobot.domain.AgentResponse;
import com.nanobot.domain.Message;
import com.nanobot.llm.LLMClient;
import com.nanobot.memory.ChatMemoryStore;
import com.nanobot.memory.MemorySummarizer;
import com.nanobot.memory.SummaryStore;
import com.nanobot.tool.ToolRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * EnhancedAgent 使用示例
 *
 * 演示滑动窗口 + 异步摘要机制的使用
 */
@Component
@ConditionalOnProperty(name = "nanobot.example.enhanced-agent.enabled", havingValue = "true")
public class EnhancedAgentExample implements CommandLineRunner {

    private final ChatMemoryStore chatMemoryStore;
    private final SummaryStore summaryStore;
    private final MemorySummarizer memorySummarizer;
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;

    public EnhancedAgentExample(
            ChatMemoryStore chatMemoryStore,
            SummaryStore summaryStore,
            MemorySummarizer memorySummarizer,
            LLMClient llmClient,
            ToolRegistry toolRegistry) {
        this.chatMemoryStore = chatMemoryStore;
        this.summaryStore = summaryStore;
        this.memorySummarizer = memorySummarizer;
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n========== EnhancedAgent 示例 ==========\n");

        // 示例 1: 基本使用
        example1_BasicUsage();

        // 示例 2: 滑动窗口机制
        example2_SlidingWindow();

        // 示例 3: 异步摘要机制
        example3_AsyncSummarization();

        System.out.println("\n========== 示例执行完成 ==========\n");
    }

    /**
     * 示例 1: 基本使用
     */
    private void example1_BasicUsage() {
        System.out.println("=== 示例 1: 基本使用 ===");

        String sessionId = "demo-session-001";
        EnhancedAgent agent = new EnhancedAgent(
                "DemoAgent",
                sessionId,
                chatMemoryStore,
                summaryStore,
                memorySummarizer,
                llmClient,
                toolRegistry,
                "你是一个友好的 AI 助手。"
        );

        // 初始化
        agent.initialize();

        // 对话
        AgentResponse response = agent.chat("你好，请介绍一下你自己");
        System.out.println("AI: " + response.content());

        System.out.println();
    }

    /**
     * 示例 2: 滑动窗口机制
     */
    private void example2_SlidingWindow() {
        System.out.println("=== 示例 2: 滑动窗口机制 ===");

        String sessionId = "demo-session-002";
        EnhancedAgent agent = new EnhancedAgent(
                "WindowAgent",
                sessionId,
                chatMemoryStore,
                summaryStore,
                memorySummarizer,
                llmClient,
                toolRegistry,
                "你是一个技术助手。",
                5  // 设置窗口大小为 5
        );

        agent.initialize();

        // 模拟多轮对话
        String[] questions = {
                "什么是 Java？",
                "Java 的特点是什么？",
                "什么是 JVM？",
                "什么是垃圾回收？",
                "什么是 Spring Boot？",
                "什么是依赖注入？",
                "现在请总结一下我们讨论的内容"  // 这时只能看到最近 5 条消息
        };

        for (String question : questions) {
            System.out.println("User: " + question);
            AgentResponse response = agent.chat(question);
            System.out.println("AI: " + response.content());
            System.out.println();

            // 显示当前上下文大小
            long messageCount = chatMemoryStore.getMessageCount(sessionId);
            System.out.println("  [当前消息总数: " + messageCount + "]");
            System.out.println();
        }

        System.out.println();
    }

    /**
     * 示例 3: 异步摘要机制
     */
    private void example3_AsyncSummarization() {
        System.out.println("=== 示例 3: 异步摘要机制 ===");

        String sessionId = "demo-session-003";
        EnhancedAgent agent = new EnhancedAgent(
                "SummaryAgent",
                sessionId,
                chatMemoryStore,
                summaryStore,
                memorySummarizer,
                llmClient,
                toolRegistry,
                "你是一个知识助手。"
        );

        agent.initialize();

        // 模拟 15 轮对话，触发摘要生成
        System.out.println("开始模拟 15 轮对话...\n");

        for (int i = 1; i <= 15; i++) {
            String question = "这是第 " + i + " 个问题";
            System.out.println("User: " + question);

            AgentResponse response = agent.chat(question);
            System.out.println("AI: " + response.content());

            long messageCount = chatMemoryStore.getMessageCount(sessionId);
            boolean hasSummary = summaryStore.hasSummary(sessionId);

            System.out.println("  [消息数: " + messageCount + ", 有摘要: " + hasSummary + "]");

            // 当消息数超过 10 条时，会触发异步摘要
            if (i == 11) {
                System.out.println("  ⚡ 触发异步摘要生成...");
                // 等待一下让异步任务完成
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 检查摘要是否生成
                if (summaryStore.hasSummary(sessionId)) {
                    String summary = summaryStore.getSummary(sessionId).orElse("");
                    System.out.println("  📝 摘要已生成: " + summary);
                }
            }

            System.out.println();
        }

        System.out.println();
    }
}
