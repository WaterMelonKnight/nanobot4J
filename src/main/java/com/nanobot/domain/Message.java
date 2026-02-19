package com.nanobot.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 消息接口 - 代表 Agent 与 LLM 之间的交互消息
 * 使用 Sealed Interface 确保类型安全
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Message.UserMessage.class, name = "user"),
    @JsonSubTypes.Type(value = Message.AssistantMessage.class, name = "assistant"),
    @JsonSubTypes.Type(value = Message.SystemMessage.class, name = "system"),
    @JsonSubTypes.Type(value = Message.ToolResultMessage.class, name = "tool_result")
})
public sealed interface Message permits
    Message.UserMessage,
    Message.AssistantMessage,
    Message.SystemMessage,
    Message.ToolResultMessage {

    String id();
    String role();
    Instant timestamp();

    /**
     * 用户消息
     */
    record UserMessage(
        String id,
        String content,
        Instant timestamp
    ) implements Message {
        public UserMessage(String content) {
            this(UUID.randomUUID().toString(), content, Instant.now());
        }

        @Override
        public String role() {
            return "user";
        }
    }

    /**
     * 助手消息（LLM 的回复）
     */
    record AssistantMessage(
        String id,
        String content,
        List<ToolCall> toolCalls,
        Instant timestamp
    ) implements Message {
        public AssistantMessage(String content, List<ToolCall> toolCalls) {
            this(UUID.randomUUID().toString(), content, toolCalls, Instant.now());
        }

        public AssistantMessage(String content) {
            this(content, List.of());
        }

        @Override
        public String role() {
            return "assistant";
        }

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    /**
     * 系统消息（用于设置 Agent 的行为）
     */
    record SystemMessage(
        String id,
        String content,
        Instant timestamp
    ) implements Message {
        public SystemMessage(String content) {
            this(UUID.randomUUID().toString(), content, Instant.now());
        }

        @Override
        public String role() {
            return "system";
        }
    }

    /**
     * 工具执行结果消息
     */
    record ToolResultMessage(
        String id,
        String toolCallId,
        String toolName,
        String result,
        boolean success,
        Instant timestamp
    ) implements Message {
        public ToolResultMessage(String toolCallId, String toolName, String result, boolean success) {
            this(UUID.randomUUID().toString(), toolCallId, toolName, result, success, Instant.now());
        }

        @Override
        public String role() {
            return "tool";
        }
    }
}
