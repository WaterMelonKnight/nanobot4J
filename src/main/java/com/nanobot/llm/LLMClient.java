package com.nanobot.llm;

import com.nanobot.domain.Message;
import com.nanobot.tool.Tool;

import java.util.List;

/**
 * LLM 客户端接口 - 封装与 LLM 的交互
 *
 * 设计思路：
 * 1. 抽象 LLM 调用，支持不同的 LLM 提供商（OpenAI、Anthropic 等）
 * 2. 使用 Spring AI 的 ChatClient 进行底层实现
 * 3. 支持工具调用（Function Calling）
 * 4. 同步阻塞风格，运行在虚拟线程中
 */
public interface LLMClient {

    /**
     * 发送消息并获取响应
     *
     * @param messages 对话历史
     * @return LLM 的响应消息
     */
    Message.AssistantMessage chat(List<Message> messages);

    /**
     * 发送消息并获取响应（支持工具调用）
     *
     * @param messages 对话历史
     * @param tools 可用的工具列表
     * @return LLM 的响应消息（可能包含工具调用请求）
     */
    Message.AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools);

    /**
     * 获取模型名称
     */
    String getModelName();
}
