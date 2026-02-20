package com.nanobot.core.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /**
     * 角色：system, user, assistant, tool
     */
    private String role;

    /**
     * 内容
     */
    private String content;

    /**
     * 工具调用 ID（如果是 tool 角色）
     */
    private String toolCallId;

    /**
     * 工具名称（如果是 tool 角色）
     */
    private String toolName;

    public static Message system(String content) {
        return new Message("system", content, null, null);
    }

    public static Message user(String content) {
        return new Message("user", content, null, null);
    }

    public static Message assistant(String content) {
        return new Message("assistant", content, null, null);
    }

    public static Message tool(String toolCallId, String toolName, String content) {
        return new Message("tool", content, toolCallId, toolName);
    }
}
