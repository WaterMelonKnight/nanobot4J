package com.nanobot.starter.annotation;

import java.lang.annotation.*;

/**
 * NanobotTool 注解 - 标记方法为可被 Agent 调用的工具
 *
 * 使用示例：
 * <pre>
 * @NanobotTool(
 *     name = "calculator",
 *     description = "执行数学计算",
 *     parameterSchema = "{\"type\":\"object\",\"properties\":{\"expression\":{\"type\":\"string\"}}}"
 * )
 * public String calculate(String expression) {
 *     // 实现计算逻辑
 *     return result;
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NanobotTool {

    /**
     * 工具名称（唯一标识符）
     */
    String name();

    /**
     * 工具描述（给 LLM 看的，描述工具的功能）
     */
    String description();

    /**
     * 参数 Schema（JSON Schema 格式）
     *
     * 示例：
     * {
     *   "type": "object",
     *   "properties": {
     *     "query": {
     *       "type": "string",
     *       "description": "搜索关键词"
     *     }
     *   },
     *   "required": ["query"]
     * }
     */
    String parameterSchema() default "{}";
}
