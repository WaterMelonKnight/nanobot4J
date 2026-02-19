package com.nanobot.llm.service;

import com.nanobot.domain.Message;
import com.nanobot.tool.Tool;

import java.util.List;

/**
 * LLM 服务接口 - 统一的 LLM 交互层
 *
 * 职责：
 * 1. 提供统一的 LLM 调用接口
 * 2. 支持动态选择模型
 * 3. 支持降级策略
 * 4. 屏蔽底层不同 API 的差异
 *
 * 设计思路：
 * - 这是一个更高层次的抽象，位于 LLMClient 之上
 * - LLMClient 负责与具体的 LLM API 交互
 * - LLMService 负责模型选择、降级、监控等业务逻辑
 */
public interface LLMService {

    /**
     * 使用默认模型进行对话
     *
     * @param messages 对话历史
     * @return LLM 的响应消息
     */
    Message.AssistantMessage chat(List<Message> messages);

    /**
     * 使用默认模型进行对话（支持工具调用）
     *
     * @param messages 对话历史
     * @param tools 可用的工具列表
     * @return LLM 的响应消息
     */
    Message.AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools);

    /**
     * 使用指定模型进行对话
     *
     * @param modelName 模型名称（如 "deepseek", "kimi", "ollama"）
     * @param messages 对话历史
     * @return LLM 的响应消息
     */
    Message.AssistantMessage chatWithModel(String modelName, List<Message> messages);

    /**
     * 使用指定模型进行对话（支持工具调用）
     *
     * @param modelName 模型名称
     * @param messages 对话历史
     * @param tools 可用的工具列表
     * @return LLM 的响应消息
     */
    Message.AssistantMessage chatWithModelAndTools(
            String modelName,
            List<Message> messages,
            List<Tool> tools);

    /**
     * 获取当前使用的模型名称
     */
    String getCurrentModelName();

    /**
     * 获取所有可用的模型名称
     */
    java.util.Set<String> getAvailableModels();
}
