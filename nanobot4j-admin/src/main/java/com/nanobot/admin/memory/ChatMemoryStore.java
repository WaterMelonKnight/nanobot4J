package com.nanobot.admin.memory;

import com.nanobot.core.llm.Message;

import java.util.List;

/**
 * 聊天记忆存储接口
 *
 * 职责：
 * 1. 按 sessionId 存储和检索对话历史
 * 2. 支持 TTL 自动过期
 * 3. 支持滑动窗口查询（最近 N 条）
 */
public interface ChatMemoryStore {

    /**
     * 添加一条消息到会话历史
     *
     * @param sessionId 会话 ID
     * @param message 消息对象
     */
    void addMessage(String sessionId, Message message);

    /**
     * 获取会话的所有历史消息
     *
     * @param sessionId 会话 ID
     * @return 消息列表（按时间升序）
     */
    List<Message> getMessages(String sessionId);

    /**
     * 获取会话的最近 N 条消息（滑动窗口）
     *
     * @param sessionId 会话 ID
     * @param limit 最大消息数
     * @return 最近的消息列表（按时间升序）
     */
    List<Message> getRecentMessages(String sessionId, int limit);

    /**
     * 获取会话的消息总数
     *
     * @param sessionId 会话 ID
     * @return 消息数量
     */
    long getMessageCount(String sessionId);

    /**
     * 清空会话的所有历史
     *
     * @param sessionId 会话 ID
     */
    void clearMessages(String sessionId);

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话 ID
     * @return 是否存在
     */
    boolean exists(String sessionId);
}
