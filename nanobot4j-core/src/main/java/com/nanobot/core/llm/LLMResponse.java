package com.nanobot.core.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LLMResponse {

    /**
     * 响应内容
     */
    private String content;

    /**
     * 工具调用请求（如果 LLM 想调用工具）
     */
    private List<ToolCall> toolCalls;

    /**
     * 是否完成
     */
    private boolean finished;

    /**
     * Token 使用情况
     */
    private TokenUsage tokenUsage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }
}
