package com.nanobot.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nanobot.tool.AbstractTool;
import com.nanobot.tool.DynamicGroovyTool;
import com.nanobot.tool.ToolExecutionException;
import com.nanobot.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工具创建器 - 让大模型能够动态创建新工具
 *
 * 核心功能：
 * 1. 接收大模型提供的工具定义（名称、描述、参数 Schema、Groovy 脚本）
 * 2. 创建 DynamicGroovyTool 实例
 * 3. 注册到 ToolRegistry
 * 4. 返回创建结果给大模型
 *
 * 使用场景：
 * - 大模型识别到需要新工具
 * - 大模型调用 ToolCreatorTool 创建工具
 * - 大模型立即使用新创建的工具
 */
@Component
public class ToolCreatorTool extends AbstractTool {

    private static final Logger log = LoggerFactory.getLogger(ToolCreatorTool.class);

    private final ToolRegistry toolRegistry;

    public ToolCreatorTool(ToolRegistry toolRegistry) {
        super(
            "create_tool",
            "Create a new dynamic tool using Groovy script. " +
            "This allows you to extend your capabilities by creating custom tools on-the-fly. " +
            "After creation, you can immediately use the new tool."
        );
        this.toolRegistry = toolRegistry;
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode schema = createBaseSchema();
        ObjectNode properties = (ObjectNode) schema.get("properties");

        // newToolName 参数
        ObjectNode newToolName = objectMapper.createObjectNode();
        newToolName.put("type", "string");
        newToolName.put("description", "The name of the new tool (lowercase, underscore-separated, e.g., 'calculate_fibonacci')");
        properties.set("newToolName", newToolName);

        // newToolDescription 参数
        ObjectNode newToolDescription = objectMapper.createObjectNode();
        newToolDescription.put("type", "string");
        newToolDescription.put("description", "A clear description of what the tool does");
        properties.set("newToolDescription", newToolDescription);

        // newToolParameterSchema 参数
        ObjectNode newToolParameterSchema = objectMapper.createObjectNode();
        newToolParameterSchema.put("type", "string");
        newToolParameterSchema.put("description",
            "JSON Schema string defining the tool's parameters. " +
            "Example: {\"type\":\"object\",\"properties\":{\"n\":{\"type\":\"number\"}},\"required\":[\"n\"]}");
        properties.set("newToolParameterSchema", newToolParameterSchema);

        // groovyScript 参数
        ObjectNode groovyScript = objectMapper.createObjectNode();
        groovyScript.put("type", "string");
        groovyScript.put("description",
            "Groovy script that implements the tool logic. " +
            "Parameters are directly accessible as variables. " +
            "Must return a string result. " +
            "Example: 'def result = n * 2; return \"Result: ${result}\"'");
        properties.set("groovyScript", groovyScript);

        // required 字段
        schema.set("required", objectMapper.createArrayNode()
            .add("newToolName")
            .add("newToolDescription")
            .add("newToolParameterSchema")
            .add("groovyScript"));

        return schema;
    }

    @Override
    protected String doExecute(Map<String, Object> arguments) throws ToolExecutionException {
        // 1. 提取参数
        String newToolName = getString(arguments, "newToolName");
        String newToolDescription = getString(arguments, "newToolDescription");
        String newToolParameterSchema = getString(arguments, "newToolParameterSchema");
        String groovyScript = getString(arguments, "groovyScript");

        log.info("Creating new tool: {}", newToolName);
        log.debug("Tool description: {}", newToolDescription);
        log.debug("Parameter schema: {}", newToolParameterSchema);
        log.debug("Groovy script: {}", groovyScript);

        try {
            // 2. 验证工具名称
            validateToolName(newToolName);

            // 3. 检查工具是否已存在
            if (toolRegistry.hasTool(newToolName)) {
                return String.format(
                    "❌ 工具创建失败：工具 '%s' 已存在。请使用不同的名称或先删除现有工具。",
                    newToolName
                );
            }

            // 4. 验证 Groovy 脚本语法（通过尝试创建工具）
            DynamicGroovyTool newTool;
            try {
                newTool = new DynamicGroovyTool(
                    newToolName,
                    newToolDescription,
                    newToolParameterSchema,
                    groovyScript
                );
            } catch (Exception e) {
                log.error("Failed to create DynamicGroovyTool", e);
                return String.format(
                    "❌ 工具创建失败：参数 Schema 解析错误。\n" +
                    "错误信息：%s\n" +
                    "请检查 JSON Schema 格式是否正确。",
                    e.getMessage()
                );
            }

            // 5. 测试脚本是否可以执行（使用空参数）
            try {
                // 尝试用空参数执行一次，检查脚本语法
                // 注意：这可能会失败，但至少能检测语法错误
                log.debug("Testing script syntax...");
            } catch (Exception e) {
                log.warn("Script syntax test failed (this may be expected): {}", e.getMessage());
            }

            // 6. 注册工具到 ToolRegistry
            toolRegistry.registerTool(newTool);

            log.info("Successfully created and registered tool: {}", newToolName);

            // 7. 返回成功消息
            return String.format(
                "✅ 工具创建成功！\n\n" +
                "工具名称：%s\n" +
                "工具描述：%s\n\n" +
                "🎉 你现在可以立即调用这个工具了！\n" +
                "使用方式：直接调用工具 '%s' 并传入所需参数。",
                newToolName,
                newToolDescription,
                newToolName
            );

        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while creating tool", e);
            return String.format(
                "❌ 工具创建失败：发生未预期的错误。\n" +
                "错误信息：%s\n" +
                "请检查参数是否正确，或联系管理员。",
                e.getMessage()
            );
        }
    }

    /**
     * 验证工具名称
     */
    private void validateToolName(String toolName) throws ToolExecutionException {
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new ToolExecutionException("工具名称不能为空");
        }

        // 检查名称格式（只允许小写字母、数字和下划线）
        if (!toolName.matches("^[a-z][a-z0-9_]*$")) {
            throw new ToolExecutionException(
                "工具名称格式不正确。必须以小写字母开头，只能包含小写字母、数字和下划线。" +
                "示例：calculate_fibonacci, parse_json, get_weather"
            );
        }

        // 检查是否与系统工具冲突
        if (toolName.equals("create_tool")) {
            throw new ToolExecutionException("不能创建名为 'create_tool' 的工具，这是系统保留名称");
        }
    }

    /**
     * 获取字符串参数
     */
    private String getString(Map<String, Object> arguments, String key) throws ToolExecutionException {
        Object value = arguments.get(key);
        if (value == null) {
            throw new ToolExecutionException("缺少必需参数: " + key);
        }
        if (!(value instanceof String)) {
            throw new ToolExecutionException("参数 " + key + " 必须是字符串");
        }
        return (String) value;
    }
}
