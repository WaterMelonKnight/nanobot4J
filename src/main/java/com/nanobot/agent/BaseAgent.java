package com.nanobot.agent;

import com.nanobot.domain.AgentResponse;
import com.nanobot.domain.Message;
import com.nanobot.domain.ToolCall;
import com.nanobot.llm.LLMClient;
import com.nanobot.memory.Memory;
import com.nanobot.tool.Tool;
import com.nanobot.tool.ToolRegistry;
import com.nanobot.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 基础 Agent 实现 - 实现"思考-规划-执行"循环
 *
 * 设计思路：
 * 1. 维护 Memory 存储对话历史
 * 2. 使用 LLMClient 与 LLM 交互
 * 3. 通过 ToolRegistry 管理和执行工具
 * 4. 实现迭代循环：LLM 思考 -> 调用工具 -> 获取结果 -> 继续思考
 * 5. 所有操作都是同步阻塞的，适合在虚拟线程中运行
 */
public class BaseAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

    private final String name;
    private final Memory memory;
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final String systemPrompt;

    private boolean initialized = false;

    public BaseAgent(
            String name,
            Memory memory,
            LLMClient llmClient,
            ToolRegistry toolRegistry,
            String systemPrompt) {
        this.name = name;
        this.memory = memory;
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public void initialize() {
        if (!initialized) {
            // 添加系统提示词
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                memory.addMessage(new Message.SystemMessage(systemPrompt));
            }
            initialized = true;
            log.info("Agent '{}' initialized", name);
        }
    }

    @Override
    public AgentResponse chat(String userMessage) {
        if (!initialized) {
            initialize();
        }

        // 添加用户消息
        memory.addMessage(new Message.UserMessage(userMessage));

        // 执行思考-规划-执行循环
        return run(10); // 默认最多 10 轮迭代
    }

    @Override
    public AgentResponse run(int maxIterations) {
        int iteration = 0;
        AgentResponse.AgentState currentState = AgentResponse.AgentState.THINKING;

        while (iteration < maxIterations) {
            iteration++;
            log.debug("Iteration {}/{}", iteration, maxIterations);

            try {
                // 1. 思考阶段：调用 LLM
                currentState = AgentResponse.AgentState.THINKING;
                List<Message> context = memory.getContext();
                List<Tool> availableTools = new ArrayList<>(toolRegistry.getAllTools());

                Message.AssistantMessage response = llmClient.chatWithTools(context, availableTools);
                memory.addMessage(response);

                // 2. 检查是否需要执行工具
                if (!response.hasToolCalls()) {
                    // 没有工具调用，说明 LLM 已经完成任务
                    currentState = AgentResponse.AgentState.COMPLETED;
                    return new AgentResponse(
                        response.content(),
                        memory.getMessages(),
                        currentState,
                        iteration
                    );
                }

                // 3. 执行阶段：执行工具调用
                currentState = AgentResponse.AgentState.EXECUTING;
                executeToolCalls(response.toolCalls());

            } catch (Exception e) {
                log.error("Error in agent iteration {}", iteration, e);
                currentState = AgentResponse.AgentState.ERROR;
                return new AgentResponse(
                    "Error: " + e.getMessage(),
                    memory.getMessages(),
                    currentState,
                    iteration
                );
            }
        }

        // 达到最大迭代次数
        log.warn("Agent reached max iterations: {}", maxIterations);
        return new AgentResponse(
            "Reached maximum iterations without completion",
            memory.getMessages(),
            AgentResponse.AgentState.COMPLETED,
            iteration
        );
    }

    /**
     * 执行工具调用
     */
    private void executeToolCalls(List<ToolCall> toolCalls) {
        for (ToolCall toolCall : toolCalls) {
            log.debug("Executing tool:  with args: ", toolCall.name(), toolCall.arguments());

            var toolOpt = toolRegistry.getTool(toolCall.name());
            if (toolOpt.isEmpty()) {
                log.warn("Tool not found: {}", toolCall.name());
                memory.addMessage(new Message.ToolResultMessage(
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
                memory.addMessage(new Message.ToolResultMessage(
                    toolCall.id(),
                    toolCall.name(),
                    result.content(),
                    result.success()
                ));
            } catch (Exception e) {
                log.error("Tool execution failed: {}", toolCall.name(), e);
                memory.addMessage(new Message.ToolResultMessage(
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
        memory.clear();
        initialized = false;
        log.info("Agent '{}' reset", name);
    }

    @Override
    public String getName() {
        return name;
    }
}
