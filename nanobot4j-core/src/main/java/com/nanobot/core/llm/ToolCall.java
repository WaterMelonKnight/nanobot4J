package com.nanobot.core.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具调用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {

    /**
     * 调用 ID
     */
    private String id;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 参数
     */
    private Map<String, Object> arguments;
}
