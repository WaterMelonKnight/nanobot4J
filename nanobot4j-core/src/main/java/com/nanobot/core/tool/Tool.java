package com.nanobot.core.tool;

import java.util.Map;

/**
 * Tool 接口 - 所有工具的基础接口
 *
 * 工具是 Agent 可以调用的外部能力，例如：
 * - 计算器
 * - 搜索引擎
 * - 数据库查询
 * - API 调用
 */
public interface Tool {

    /**
     * 获取工具名称
     * @return 工具的唯一标识符
     */
    String getName();

    /**
     * 获取工具描述（给 LLM 看的）
     * @return 工具的功能描述
     */
    String getDescription();

    /**
     * 获取参数 Schema（JSON Schema 格式）
     * @return 参数定义
     */
    String getParameterSchema();

    /**
     * 执行工具
     * @param parameters 参数 Map
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> parameters);
}
