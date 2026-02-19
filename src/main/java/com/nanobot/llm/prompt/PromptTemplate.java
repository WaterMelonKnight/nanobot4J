package com.nanobot.llm.prompt;

import com.nanobot.domain.Message;

import java.util.List;

/**
 * Prompt 模板接口
 * 用于将消息列表格式化为不同模型可以理解的格式
 */
public interface PromptTemplate {

    /**
     * 格式化消息列表
     *
     * @param messages 消息列表
     * @return 格式化后的字符串
     */
    String format(List<Message> messages);

    /**
     * 获取模板名称
     */
    String getName();
}
