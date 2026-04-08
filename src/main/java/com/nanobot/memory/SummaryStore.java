package com.nanobot.memory;

import java.util.Optional;

/**
 * 对话摘要存储接口
 *
 * 用于存储和检索会话的历史摘要，减少上下文 token 消耗
 */
public interface SummaryStore {

    /**
     * 保存会话摘要
     *
     * @param sessionId 会话ID
     * @param summary 摘要内容
     */
    void saveSummary(String sessionId, String summary);

    /**
     * 获取会话摘要
     *
     * @param sessionId 会话ID
     * @return 摘要内容（如果存在）
     */
    Optional<String> getSummary(String sessionId);

    /**
     * 删除会话摘要
     *
     * @param sessionId 会话ID
     */
    void deleteSummary(String sessionId);

    /**
     * 检查会话是否有摘要
     *
     * @param sessionId 会话ID
     * @return 是否存在摘要
     */
    boolean hasSummary(String sessionId);

    /**
     * 刷新摘要的 TTL
     *
     * @param sessionId 会话ID
     */
    void refreshTTL(String sessionId);
}
