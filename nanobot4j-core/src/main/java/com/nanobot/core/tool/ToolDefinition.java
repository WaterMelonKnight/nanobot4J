package com.nanobot.core.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool 定义 - 用于注册和传递给 LLM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 参数 Schema (JSON Schema 格式)
     */
    private String parameterSchema;

    /**
     * 工具实例
     */
    private Tool tool;

    /**
     * 从 Tool 实例创建定义
     */
    public static ToolDefinition from(Tool tool) {
        return new ToolDefinition(
            tool.getName(),
            tool.getDescription(),
            tool.getParameterSchema(),
            tool
        );
    }
}
