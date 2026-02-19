package com.nanobot.agent;

import com.nanobot.domain.AgentResponse;
import com.nanobot.llm.LLMClient;
import com.nanobot.memory.Memory;
import com.nanobot.tool.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * Agent 工厂 - 简化 Agent 创建
 */
@Component
public class AgentFactory {

    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;

    public AgentFactory(LLMClient llmClient, ToolRegistry toolRegistry) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
    }

    /**
     * 创建一个基础 Agent
     */
    public Agent createAgent(String name, Memory memory, String systemPrompt) {
        return new BaseAgent(name, memory, llmClient, toolRegistry, systemPrompt);
    }

    /**
     * 创建一个预配置的数学助手
     */
    public Agent createMathAssistant(Memory memory) {
        return createAgent(
            "MathAssistant",
            memory,
            """
            You are a helpful math assistant.
            You can perform calculations using the calculator tool.
            Always show your work and explain your reasoning.
            """
        );
    }

    /**
     * 创建一个通用助手
     */
    public Agent createGeneralAssistant(Memory memory) {
        return createAgent(
            "GeneralAssistant",
            memory,
            """
            You are a helpful AI assistant.
            You have access to various tools to help answer questions.
            Be concise but thorough in your responses.
            """
        );
    }
}
