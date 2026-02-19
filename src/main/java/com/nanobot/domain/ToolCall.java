package com.nanobot.domain;

import java.util.Map;
import java.util.UUID;

/**
 * 工具调用请求 - LLM 请求调用某个工具
 */
public record ToolCall(
    String id,
    String name,
    Map<String, Object> arguments
) {
    public ToolCall(String name, Map<String, Object> arguments) {
        this(UUID.randomUUID().toString(), name, arguments);
    }
}
