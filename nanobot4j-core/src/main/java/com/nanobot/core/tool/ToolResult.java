package com.nanobot.core.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool 执行结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果数据
     */
    private Object data;

    /**
     * 错误消息（如果失败）
     */
    private String error;

    /**
     * 创建成功结果
     */
    public static ToolResult success(Object data) {
        return new ToolResult(true, data, null);
    }

    /**
     * 创建失败结果
     */
    public static ToolResult failure(String error) {
        return new ToolResult(false, null, error);
    }
}
