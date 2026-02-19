package com.nanobot.agent;

import com.nanobot.domain.AgentResponse;

/**
 * Agent 接口 - 定义 Agent 的生命周期和核心行为
 *
 * 设计思路：
 * 1. Agent 是一个有状态的对象，维护自己的 Memory
 * 2. 支持单轮和多轮对话
 * 3. 实现"思考-规划-执行"循环
 * 4. 所有操作都是同步阻塞的，运行在虚拟线程中
 */
public interface Agent {

    /**
     * 初始化 Agent
     * 可以在这里设置系统提示词、加载工具等
     */
    void initialize();

    /**
     * 处理用户消息并返回响应
     *
     * @param userMessage 用户输入
     * @return Agent 的响应
     */
    AgentResponse chat(String userMessage);

    /**
     * 执行一轮"思考-规划-执行"循环
     *
     * @param maxIterations 最大迭代次数（防止无限循环）
     * @return Agent 的响应
     */
    AgentResponse run(int maxIterations);

    /**
     * 重置 Agent 状态（清空 Memory）
     */
    void reset();

    /**
     * 获取 Agent 的名称
     */
    String getName();
}
