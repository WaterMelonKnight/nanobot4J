package com.nanobot.core.memory;

import com.nanobot.core.llm.Message;

import java.util.List;

/**
 * Memory 接口 - 管理对话历史
 */
public interface Memory {

    /**
     * 添加消息到记忆
     * @param message 消息
     */
    void addMessage(Message message);

    /**
     * 获取所有消息
     * @return 消息列表
     */
    List<Message> getMessages();

    /**
     * 清空记忆
     */
    void clear();

    /**
     * 获取最近 N 条消息
     * @param n 数量
     * @return 消息列表
     */
    List<Message> getRecentMessages(int n);
}
