package com.nanobot.tool;

import java.time.Duration;
import java.time.Instant;

/**
 * 工具执行结果
 */
public record ToolResult(
    String content,
    boolean success,
    String errorMessage,
    Instant startTime,
    Instant endTime
) {
    public ToolResult(String content, boolean success) {
        this(content, success, null, Instant.now(), Instant.now());
    }

    public static ToolResult success(String content) {
        return new ToolResult(content, true, null, Instant.now(), Instant.now());
    }

    public static ToolResult failure(String errorMessage) {
        return new ToolResult(null, false, errorMessage, Instant.now(), Instant.now());
    }

    public Duration executionTime() {
        return Duration.between(startTime, endTime);
    }
}
