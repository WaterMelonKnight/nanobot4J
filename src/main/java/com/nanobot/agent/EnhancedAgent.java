package com.nanobot.agent;

import com.nanobot.domain.AgentResponse;
import com.nanobot.domain.Message;
import com.nanobot.domain.ToolCall;
import com.nanobot.llm.LLMClient;
import com.nanobot.memory.ChatMemoryStore;
import com.nanobot.memory.MemorySummarizer;
import com.nanobot.memory.SummaryStore;
import com.nanobot.tool.Tool;
import com.nanobot.tool.ToolRegistry;
import com.nanobot.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 增强版 Agent - 支持滑动窗口和异步摘要机制
 *
 * 核心优化：
 * 1. 滑动窗口：每次只携带最近 N 条消息（默认 10 条）
 * 2. 异步摘要：当消息数超过阈值时，异步生成历史摘要
 * 3. 智能上下文：将摘要作为 System Prompt 注入，节省 token
 * 4. 使用 ChatMemoryStore 支持多会话管理
 */
public class EnhancedAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(EnhancedAgent.class);

    private static final int DEFAULT_WINDOW_SIZE = 10; // 滑动窗口大小
    private static final int SUMMARY_THRESHOLD = 10;   // 触发摘要的消息数阈值

    private final String name;
    private final String sessionId;
    private final ChatMemoryStore memoryStore;
    private final SummaryStore summaryStore;
    private final MemorySummarizer memorySummarizer;
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final String systemPrompt;
    private final int windowSize;

    private boolean initialized = false;

    public EnhancedAgent(
            String name,
            String sessionId,
            ChatMemoryStore memoryStore,
            SummaryStore summaryStore,
            MemorySummarizer memorySummarizer,
            LLMClient llmClient,
            ToolRegistry toolRegistry,
            String systemPrompt) {
        this(name, sessionId, memoryStore, summaryStore, memorySummarizer,
                llmClient, toolRegistry, systemPrompt, DEFAULT_WINDOW_SIZE);
    }

    public EnhancedAgent(
            String name,
            String sessionId,
            ChatMemoryStore memoryStore,
            SummaryStore summaryStore,
            MemorySummarizer memorySummarizer,
            LLMClient llmClient,
            ToolRegistry toolRegistry,
            String systemPrompt,
            int windowSize) {
        this.name = name;
        this.sessionId = sessionId;
        this.memoryStore = memoryStore;
        this.summaryStore = summaryStore;
        this.memorySummarizer = memorySummarizer;
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.systemPrompt = systemPrompt;
        this.windowSize = windowSize;
    }

    @Override
    public void initialize() {
        if (!initialized) {
            // 添加系统提示词
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                memoryStore.addMessage(sessionId, new Message.SystemMessage(systemPrompt));
            }
            initialized = true;
            log.info("EnhancedAgent '{}' initialized for session {}", name, sessionId);
        }
    }

    @Override
    public AgentResponse chat(String userMessage) {
        if (!initialized) {
            initialize();
        }

        // 添加用户消息
        memoryStore.addMessage(sessionId, new Message.UserMessage(userMessage));

        // 检查是否需要生成摘要
        checkAndTriggerSummarization();

        // 执行思考-规划-执行循环
        return run(10); // 默认最多 10 轮迭代
    }

    @Override
    public AgentResponse run(int maxIterations) {
        int iteration = 0;
        AgentResponse.AgentState currentState = AgentResponse.AgentState.THINKING;

        while (iteration < maxIterations) {
            iteration++;
            log.debug("Iteration {}/{} for session {}", iteration, maxIterations, sessionId);

            try {
                // 1. 思考阶段：调用 LLM
                currentState = AgentResponse.AgentState.THINKING;

                // 构建上下文（滑动窗口 + 摘要）
                List<Message> context = buildContext();
                List<Tool> availableTools = new ArrayList<>(toolRegistry.getAllTools());

                Message.AssistantMessage response = llmClient.chatWithTools(context, availableTools);
                memoryStore.addMessage(sessionId, response);

                // 2. 检查是否需要执行工具
                if (!response.hasToolCalls()) {
                    // 没有工具调用，说明 LLM 已经完成任务
                    currentState = AgentResponse.AgentState.COMPLETED;
                    return new AgentResponse(
                            response.content(),
                            memoryStore.getRecentMessages(sessionId, windowSize),
                            currentState,
                            iteration
                    );
                }

                // 3. 执行阶段：执行工具调用
                currentState = AgentResponse.AgentState.EXECUTING;
                executeToolCalls(response.toolCalls());

            } catch (Exception e) {
                log.error("Error in agent iteration {} for session {}", iteration, sessionId, e);
                currentState = AgentResponse.AgentState.ERROR;
                return new AgentResponse(
                        "Error: " + e.getMessage(),
                        memoryStore.getRecentMessages(sessionId, windowSize),
                        currentState,
                        iteration
                );
            }
        }

        // 达到最大迭代次数
        log.warn("Agent reached max iterations: {} for session {}", maxIterations, sessionId);
        return new AgentResponse(
                "Reached maximum iterations without completion",
                memoryStore.getRecentMessages(sessionId, windowSize),
                AgentResponse.AgentState.COMPLETED,
                iteration
        );
    }

    /**
     * 构建上下文（滑动窗口 + 摘要）
     */
    private List<Message> buildContext() {
        List<Message> context = new ArrayList<>();

        // 1. 获取历史摘要（如果存在）
        Optional<String> summaryOpt = summaryStore.getSummary(sessionId);
        if (summaryOpt.isPresent()) {
            String summaryPrompt = "【历史对话摘要】\n" + summaryOpt.get() +
                    "\n\n以上是之前的对话摘要，请基于此继续对话。";
            context.add(new Message.SystemMessage(summaryPrompt));
            log.debug("Added summary to context for session {}", sessionId);
        }

        // 2. 获取最近的 N 条消息（滑动窗口）
        List<Message> recentMessages = memoryStore.getRecentMessages(sessionId, windowSize);
        context.addAll(recentMessages);

        log.debug("Built context for session {}: {} messages (summary: {})",
                sessionId, context.size(), summaryOpt.isPresent());

        return context;
    }

    /**
     * 检查并触发摘要生成
     */
    private void checkAndTriggerSummarization() {
        long messageCount = memoryStore.getMessageCount(sessionId);
        boolean hasSummary = summaryStore.hasSummary(sessionId);

        // 判断是否需要生成摘要
        if (memorySummarizer.shouldSummarize(messageCount, SUMMARY_THRESHOLD, hasSummary)) {
            log.info("Triggering async summarization for session {} ({} messages)",
                    sessionId, messageCount);

            // 获取前 N 条消息用于摘要
            List<Message> allMessages = memoryStore.getAllMessages(sessionId);
            int summaryEndIndex = Math.min(SUMMARY_THRESHOLD, allMessages.size());
            List<Message> messagesToSummarize = allMessages.subList(0, summaryEndIndex);

            // 异步生成摘要（使用虚拟线程）
            memorySummarizer.summarizeAsync(sessionId, messagesToSummarize)
                    .thenAccept(summary -> {
                        log.info("Summarization completed for session {}: {} chars",
                                sessionId, summary.length());
                    })
                    .exceptionally(ex -> {
                        log.error("Summarization failed for session {}", sessionId, ex);
                        return null;
                    });
        }
    }

    /**
     * 执行工具调用
     */
    private void executeToolCalls(List<ToolCall> toolCalls) {
        for (ToolCall toolCall : toolCalls) {
            log.debug("Executing tool: {} with args: {}", toolCall.name(), toolCall.arguments());

            var toolOpt = toolRegistry.getTool(toolCall.name());
            if (toolOpt.isEmpty()) {
                log.warn("Tool not found: {}", toolCall.name());
                memoryStore.addMessage(sessionId, new Message.ToolResultMessage(
                        toolCall.id(),
                        toolCall.name(),
                        "Error: Tool not found",
                        false
                ));
                continue;
            }

            Tool tool = toolOpt.get();
            try {
                ToolResult result = tool.execute(toolCall.arguments());
                memoryStore.addMessage(sessionId, new Message.ToolResultMessage(
                        toolCall.id(),
                        toolCall.name(),
                        result.content(),
                        result.success()
                ));
            } catch (Exception e) {
                log.error("Tool execution failed: {}", toolCall.name(), e);
                memoryStore.addMessage(sessionId, new Message.ToolResultMessage(
                        toolCall.id(),
                        toolCall.name(),
                        "Error: " + e.getMessage(),
                        false
                ));
            }
        }
    }

    @Override
    public void reset() {
        memoryStore.clear(sessionId);
        summaryStore.deleteSummary(sessionId);
        initialized = false;
        log.info("EnhancedAgent '{}' reset for session {}", name, sessionId);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getSessionId() {
        return sessionId;
    }
}
