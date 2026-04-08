package com.nanobot.example;

import com.nanobot.agent.BaseAgent;
import com.nanobot.agent.EnhancedAgent;
import com.nanobot.domain.AgentResponse;
import com.nanobot.llm.LLMClient;
import com.nanobot.memory.ChatMemoryStore;
import com.nanobot.memory.InMemoryMemory;
import com.nanobot.memory.MemorySummarizer;
import com.nanobot.memory.SummaryStore;
import com.nanobot.tool.ToolRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Token 优化效果对比测试
 *
 * 对比传统 Agent 和 EnhancedAgent 的 Token 消耗
 */
@Component
@ConditionalOnProperty(name = "nanobot.example.token-comparison.enabled", havingValue = "true")
public class TokenComparisonExample implements CommandLineRunner {

    private final ChatMemoryStore chatMemoryStore;
    private final SummaryStore summaryStore;
    private final MemorySummarizer memorySummarizer;
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;

    public TokenComparisonExample(
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
        System.out.println("\n========== Token 优化效果对比 ==========\n");

        // 测试场景：20 轮对话
        int rounds = 20;

        // 测试 1: 传统 Agent（无优化）
        System.out.println("=== 测试 1: 传统 Agent（无优化）===");
        testTraditionalAgent(rounds);

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试 2: EnhancedAgent（滑动窗口 + 摘要）
        System.out.println("=== 测试 2: EnhancedAgent（滑动窗口 + 摘要）===");
        testEnhancedAgent(rounds);

        System.out.println("\n========== 对比完成 ==========\n");
    }

    /**
     * 测试传统 Agent
     */
    private void testTraditionalAgent(int rounds) {
        InMemoryMemory memory = new InMemoryMemory();
        BaseAgent agent = new BaseAgent(
                "TraditionalAgent",
                memory,
                llmClient,
                toolRegistry,
                "你是一个技术助手。"
        );

        agent.initialize();

        int totalMessages = 0;
        int estimatedTokens = 0;

        for (int i = 1; i <= rounds; i++) {
            String question = "这是第 " + i + " 个技术问题";
            AgentResponse response = agent.chat(question);

            totalMessages = memory.size();
            estimatedTokens = totalMessages * 100; // 假设每条消息 100 tokens

            if (i % 5 == 0) {
                System.out.printf("第 %d 轮 - 消息数: %d, 预估 Tokens: %d\n",
                        i, totalMessages, estimatedTokens);
            }
        }

        System.out.println("\n最终统计:");
        System.out.println("  总消息数: " + totalMessages);
        System.out.println("  预估 Token 消耗: " + estimatedTokens);
    }

    /**
     * 测试 EnhancedAgent
     */
    private void testEnhancedAgent(int rounds) throws InterruptedException {
        String sessionId = "comparison-session";
        EnhancedAgent agent = new EnhancedAgent(
                "EnhancedAgent",
                sessionId,
                chatMemoryStore,
                summaryStore,
                memorySummarizer,
                llmClient,
                toolRegistry,
                "你是一个技术助手。",
                10  // 窗口大小
        );

        agent.initialize();

        long totalMessages = 0;
        int contextSize = 0;
        int estimatedTokens = 0;

        for (int i = 1; i <= rounds; i++) {
            String question = "这是第 " + i + " 个技术问题";
            AgentResponse response = agent.chat(question);

            totalMessages = chatMemoryStore.getMessageCount(sessionId);
            boolean hasSummary = summaryStore.hasSummary(sessionId);

            // 计算实际发送的上下文大小
            contextSize = Math.min(10, (int) totalMessages);
            if (hasSummary) {
                contextSize += 1; // 摘要算 1 条消息
            }

            estimatedTokens = contextSize * 100;

            if (i % 5 == 0) {
                System.out.printf("第 %d 轮 - 总消息: %d, 上下文: %d, 有摘要: %s, 预估 Tokens: %d\n",
                        i, totalMessages, contextSize, hasSummary ? "是" : "否", estimatedTokens);
            }

            // 在第 11 轮等待摘要生成
            if (i == 11) {
                System.out.println("  ⚡ 等待异步摘要生成...");
                Thread.sleep(2000);
            }
        }

        System.out.println("\n最终统计:");
        System.out.println("  总消息数: " + totalMessages);
        System.out.println("  上下文大小: " + contextSize);
        System.out.println("  有摘要: " + (summaryStore.hasSummary(sessionId) ? "是" : "否"));
        System.out.println("  预估 Token 消耗: " + estimatedTokens);

        // 计算节省比例
        int traditionalTokens = (int) totalMessages * 100;
        int saved = traditionalTokens - estimatedTokens;
        double savePercentage = (saved * 100.0) / traditionalTokens;

        System.out.println("\n优化效果:");
        System.out.println("  传统方式 Tokens: " + traditionalTokens);
        System.out.println("  优化后 Tokens: " + estimatedTokens);
        System.out.println("  节省 Tokens: " + saved);
        System.out.println("  节省比例: " + String.format("%.2f%%", savePercentage));
    }
}
