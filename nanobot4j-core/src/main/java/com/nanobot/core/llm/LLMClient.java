package com.nanobot.core.llm;

/**
 * LLM 客户端接口
 */
public interface LLMClient {

    /**
     * 发送请求到 LLM
     * @param request LLM 请求
     * @return LLM 响应
     */
    LLMResponse chat(LLMRequest request);

    /**
     * 获取模型名称
     * @return 模型名称
     */
    String getModelName();
}
