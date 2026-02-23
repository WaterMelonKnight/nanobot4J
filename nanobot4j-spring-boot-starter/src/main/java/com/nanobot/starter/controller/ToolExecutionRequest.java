package com.nanobot.starter.controller;

import lombok.Data;

import java.util.Map;

/**
 * 工具执行请求
 */
@Data
public class ToolExecutionRequest {
    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具参数
     */
    private Map<String, Object> params;
}
