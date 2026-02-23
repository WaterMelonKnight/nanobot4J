package com.nanobot.admin.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * Agent 流式事件 - 用于 SSE 推送
 *
 * 事件类型说明：
 * - THINKING: 大模型思考中
 * - TOOL_CALL: 准备调用工具
 * - TOOL_RESULT: 工具执行完毕
 * - FINAL_ANSWER: 最终答案
 * - DONE: 任务结束
 * - ERROR: 异常
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentStreamEvent(
    EventType type,
    String content,
    String toolName,
    String toolArgs,
    String toolResult,
    Long timestamp
) {

    public enum EventType {
        THINKING,
        TOOL_CALL,
        TOOL_RESULT,
        FINAL_ANSWER,
        DONE,
        ERROR
    }

    // 便捷构造方法
    public static AgentStreamEvent thinking(String content) {
        return AgentStreamEvent.builder()
            .type(EventType.THINKING)
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static AgentStreamEvent toolCall(String toolName, String toolArgs) {
        return AgentStreamEvent.builder()
            .type(EventType.TOOL_CALL)
            .toolName(toolName)
            .toolArgs(toolArgs)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static AgentStreamEvent toolResult(String toolName, String toolResult) {
        return AgentStreamEvent.builder()
            .type(EventType.TOOL_RESULT)
            .toolName(toolName)
            .toolResult(toolResult)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static AgentStreamEvent finalAnswer(String content) {
        return AgentStreamEvent.builder()
            .type(EventType.FINAL_ANSWER)
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static AgentStreamEvent done() {
        return AgentStreamEvent.builder()
            .type(EventType.DONE)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static AgentStreamEvent error(String message) {
        return AgentStreamEvent.builder()
            .type(EventType.ERROR)
            .content(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
