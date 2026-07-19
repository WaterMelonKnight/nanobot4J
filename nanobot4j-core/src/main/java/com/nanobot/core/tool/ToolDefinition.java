package com.nanobot.core.tool;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool 定义 - 用于注册和传递给 LLM
 */
@Data
@NoArgsConstructor
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

    public ToolDefinition(String name, String description, String parameterSchema, Tool tool) {
        this.name = name;
        this.description = description;
        this.parameterSchema = parameterSchema;
        this.tool = tool;
    }

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
