package com.nanobot.core.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LLMRequest {

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 可用的工具列表
     */
    private List<ToolCall> tools;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 最大 Token 数
     */
    private Integer maxTokens;
}
