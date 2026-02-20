package com.nanobot.core.agent;

import com.nanobot.core.llm.LLMClient;
import com.nanobot.core.memory.Memory;
import com.nanobot.core.tool.ToolDefinition;

import java.util.List;
import java.util.Map;

/**
 * Agent 接口
 */
public interface Agent {

    /**
     * 处理用户输入
     * @param userInput 用户输入
     * @return Agent 响应
     */
    String chat(String userInput);

    /**
     * 获取 Agent 名称
     * @return 名称
     */
    String getName();

    /**
     * 获取系统提示词
     * @return 系统提示词
     */
    String getSystemPrompt();

    /**
     * 获取可用工具列表
     * @return 工具列表
     */
    List<ToolDefinition> getTools();

    /**
     * 注册工具
     * @param tool 工具定义
     */
    void registerTool(ToolDefinition tool);

    /**
     * 获取 Memory
     * @return Memory 实例
     */
    Memory getMemory();

    /**
     * 获取 LLM 客户端
     * @return LLM 客户端
     */
    LLMClient getLLMClient();
}
