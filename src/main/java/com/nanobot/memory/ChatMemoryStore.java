package com.nanobot.memory;

import com.nanobot.domain.Message;

import java.util.List;

/**
 * 企业级聊天记忆存储接口 - 支持多会话管理
 *
 * 设计思路：
 * 1. 支持多会话隔离，每个 sessionId 独立存储
 * 2. 支持基于 token 数量的智能上下文管理
 * 3. 支持 TTL 自动过期机制
 * 4. 支持不同的存储后端（Redis、数据库等）
 */
public interface ChatMemoryStore {

    /**
     * 添加消息到指定会话
     *
     * @param sessionId 会话ID
     * @param message 消息对象
     */
    void addMessage(String sessionId, Message message);

    /**
     * 批量添加消息到指定会话
     *
     * @param sessionId 会话ID
     * @param messages 消息列表
     */
    void addMessages(String sessionId, List<Message> messages);

    /**
     * 获取指定会话的消息列表（基于 token 限制）
     *
     * @param sessionId 会话ID
     * @param maxTokens 最大 token 数量（用于控制上下文窗口大小）
     * @return 消息列表（从最新的消息开始，直到达到 token 限制）
     */
    List<Message> getMessages(String sessionId, int maxTokens);

    /**
     * 获取指定会话的所有消息
     *
     * @param sessionId 会话ID
     * @return 完整的消息列表
     */
    List<Message> getAllMessages(String sessionId);

    /**
     * 获取指定会话的最近 N 条消息
     *
     * @param sessionId 会话ID
     * @param count 消息数量
     * @return 最近的消息列表
     */
    List<Message> getRecentMessages(String sessionId, int count);

    /**
     * 清空指定会话的所有消息
     *
     * @param sessionId 会话ID
     */
    void clear(String sessionId);

    /**
     * 删除指定会话
     *
     * @param sessionId 会话ID
     */
    void deleteSession(String sessionId);

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 是否存在
     */
    boolean exists(String sessionId);

    /**
     * 获取指定会话的消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    long getMessageCount(String sessionId);

    /**
     * 刷新会话的 TTL（延长过期时间）
     *
     * @param sessionId 会话ID
     */
    void refreshTTL(String sessionId);
}
