package com.nanobot.domain;

import java.util.List;

/**
 * Agent 的响应结果
 */
public record AgentResponse(
    String content,
    List<Message> conversationHistory,
    AgentState state,
    int iterationCount
) {
    public enum AgentState {
        THINKING,      // 思考中
        PLANNING,      // 规划中
        EXECUTING,     // 执行工具中
        COMPLETED,     // 完成
        ERROR          // 错误
    }

    public boolean isCompleted() {
        return state == AgentState.COMPLETED;
    }

    public boolean hasError() {
        return state == AgentState.ERROR;
    }
}
