package com.nanobot.admin.memory;

/**
 * 摘要存储接口
 *
 * 职责：
 * 1. 存储会话的历史摘要
 * 2. 支持摘要的更新和检索
 */
public interface SummaryStore {

    /**
     * 保存会话摘要
     *
     * @param sessionId 会话 ID
     * @param summary 摘要内容
     */
    void saveSummary(String sessionId, String summary);

    /**
     * 获取会话摘要
     *
     * @param sessionId 会话 ID
     * @return 摘要内容，如果不存在返回 null
     */
    String getSummary(String sessionId);

    /**
     * 检查会话是否有摘要
     *
     * @param sessionId 会话 ID
     * @return 是否存在摘要
     */
    boolean hasSummary(String sessionId);

    /**
     * 删除会话摘要
     *
     * @param sessionId 会话 ID
     */
    void deleteSummary(String sessionId);
}
