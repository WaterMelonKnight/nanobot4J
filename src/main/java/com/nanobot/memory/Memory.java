package com.nanobot.memory;

import com.nanobot.domain.Message;

import java.util.List;

/**
 * Memory 接口 - 管理 Agent 的对话历史和上下文
 *
 * 设计思路：
 * 1. 负责存储和检索对话历史
 * 2. 支持上下文窗口管理（避免超出 LLM 的 token 限制）
 * 3. 支持不同的存储策略（内存、数据库等）
 */
public interface Memory {

    /**
     * 添加消息到历史记录
     */
    void addMessage(Message message);

    /**
     * 批量添加消息
     */
    void addMessages(List<Message> messages);

    /**
     * 获取完整的对话历史
     */
    List<Message> getMessages();

    /**
     * 获取最近 N 条消息
     */
    List<Message> getRecentMessages(int count);

    /**
     * 获取用于 LLM 的上下文（可能经过压缩或截断）
     */
    List<Message> getContext();

    /**
     * 清空历史记录
     */
    void clear();

    /**
     * 获取消息总数
     */
    int size();

    /**
     * 检查是否为空
     */
    boolean isEmpty();
}
