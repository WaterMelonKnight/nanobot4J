package com.nanobot.tool;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Tool 接口 - 定义 Agent 可以调用的工具
 *
 * 设计思路：
 * 1. 每个工具都有唯一的名称和描述
 * 2. getSchema() 返回 JSON Schema，用于告诉 LLM 如何调用这个工具
 * 3. execute() 执行工具逻辑，返回结果字符串
 * 4. 所有工具都是同步阻塞的，运行在虚拟线程中
 */
public interface Tool {

    /**
     * 工具的唯一名称
     */
    String getName();

    /**
     * 工具的描述，用于 LLM 理解工具的用途
     */
    String getDescription();

    /**
     * 获取工具的参数 Schema（JSON Schema 格式）
     * 用于告诉 LLM 这个工具需要什么参数
     *
     * @return JSON Schema 对象
     */
    JsonNode getSchema();

    /**
     * 执行工具逻辑
     *
     * @param arguments 工具参数（从 LLM 的 tool call 中提取）
     * @return 工具执行结果
     * @throws ToolExecutionException 工具执行失败时抛出
     */
    ToolResult execute(Map<String, Object> arguments) throws ToolExecutionException;

    /**
     * 验证参数是否合法（可选，默认不验证）
     */
    default void validateArguments(Map<String, Object> arguments) throws ToolExecutionException {
        // 默认不做验证
    }
}
