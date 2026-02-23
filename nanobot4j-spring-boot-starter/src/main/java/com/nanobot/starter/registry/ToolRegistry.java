package com.nanobot.starter.registry;

import com.nanobot.core.tool.Tool;
import com.nanobot.core.tool.ToolDefinition;
import com.nanobot.core.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool 注册表 - 管理所有注册的工具
 */
@Slf4j
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();

    /**
     * 注册工具
     */
    public void registerTool(ToolDefinition toolDefinition) {
        String name = toolDefinition.getName();
        if (tools.containsKey(name)) {
            log.warn("Tool already registered, overwriting: {}", name);
        }
        tools.put(name, toolDefinition);
        log.info("Registered tool: {} - {}", name, toolDefinition.getDescription());
    }

    /**
     * 注册方法工具
     */
    public void registerMethodTool(String name, String description, String parameterSchema,
                                     Object bean, Method method) {
        MethodTool tool = new MethodTool(name, description, parameterSchema, bean, method);
        registerTool(ToolDefinition.from(tool));
    }

    /**
     * 获取工具
     */
    public ToolDefinition getTool(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有工具
     */
    public Map<String, ToolDefinition> getAllTools() {
        return new ConcurrentHashMap<>(tools);
    }

    /**
     * 执行工具
     * @param toolName 工具名称
     * @param params 参数
     * @return 执行结果
     */
    public ToolResult executeTool(String toolName, Map<String, Object> params) {
        ToolDefinition toolDef = tools.get(toolName);
        if (toolDef == null) {
            log.error("Tool not found: {}", toolName);
            return ToolResult.failure("Tool not found: " + toolName);
        }

        Tool tool = toolDef.getTool();
        if (tool == null) {
            log.error("Tool implementation not found: {}", toolName);
            return ToolResult.failure("Tool implementation not found: " + toolName);
        }

        return tool.execute(params);
    }

    /**
     * 方法工具 - 将 Spring Bean 的方法包装为 Tool
     */
    @Slf4j
    private static class MethodTool implements Tool {
        private final String name;
        private final String description;
        private final String parameterSchema;
        private final Object bean;
        private final Method method;

        public MethodTool(String name, String description, String parameterSchema,
                          Object bean, Method method) {
            this.name = name;
            this.description = description;
            this.parameterSchema = parameterSchema;
            this.bean = bean;
            this.method = method;
            this.method.setAccessible(true);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getParameterSchema() {
            return parameterSchema;
        }

        @Override
        public ToolResult execute(Map<String, Object> parameters) {
            try {
                // 简化版：假设方法接受 Map 参数
                Object result = method.invoke(bean, parameters);
                return ToolResult.success(result);
            } catch (Exception e) {
                log.error("Failed to execute tool: {}", name, e);
                return ToolResult.failure("Tool execution failed: " + e.getMessage());
            }
        }
    }
}
