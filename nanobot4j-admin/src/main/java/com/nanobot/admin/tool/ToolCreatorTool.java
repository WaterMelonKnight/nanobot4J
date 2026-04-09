package com.nanobot.admin.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工具创建器 - 允许 LLM 动态创建新工具
 *
 * 这是 Agent 自我编程能力的核心组件。
 *
 * 工作流程：
 * 1. LLM 发现现有工具无法完成任务
 * 2. LLM 调用 ToolCreatorTool，传入工具名称、描述和 Groovy 代码
 * 3. ToolCreatorTool 创建 DynamicGroovyTool 实例并注册
 * 4. LLM 立即调用新创建的工具完成任务
 *
 * 参数格式：
 * {
 *   "tool_name": "工具名称",
 *   "description": "工具描述",
 *   "groovy_code": "Groovy 脚本代码"
 * }
 *
 * 示例：
 * {
 *   "tool_name": "fibonacci",
 *   "description": "计算斐波那契数列第 n 项",
 *   "groovy_code": "def fib(n) { n <= 1 ? n : fib(n-1) + fib(n-2) }; return fib(n as int)"
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolCreatorTool {

    private final DynamicToolRegistry dynamicToolRegistry;
    private final ObjectMapper objectMapper;

    /**
     * 工具名称
     */
    public static final String TOOL_NAME = "create_tool";

    /**
     * 工具描述
     */
    public static final String DESCRIPTION =
        "动态创建新工具。参数：tool_name(工具名称), description(工具描述), groovy_code(Groovy脚本代码)。" +
        "脚本可以访问传入的参数变量。返回值会自动转换为字符串。";

    /**
     * 参数 Schema（JSON Schema 格式）
     */
    public static final String PARAMETER_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "tool_name": {
              "type": "string",
              "description": "新工具的名称（小写字母和下划线）"
            },
            "description": {
              "type": "string",
              "description": "工具的功能描述"
            },
            "groovy_code": {
              "type": "string",
              "description": "Groovy 脚本代码，可以访问传入的参数变量"
            }
          },
          "required": ["tool_name", "description", "groovy_code"]
        }
        """;

    /**
     * 执行工具创建
     *
     * @param parameters 参数 Map
     * @return 创建结果
     */
    public String execute(Map<String, Object> parameters) {
        log.info("ToolCreatorTool invoked with parameters: ", parameters);

        try {
            // 1. 提取参数
            String toolName = extractParameter(parameters, "tool_name");
            String description = extractParameter(parameters, "description");
            String groovyCode = extractParameter(parameters, "groovy_code");

            // 2. 验证参数
            validateParameters(toolName, description, groovyCode);

            // 3. 检查工具是否已存在
            if (dynamicToolRegistry.hasTool(toolName)) {
                log.warn("Tool already exists: {}", toolName);
                return "Warning: Tool '" + toolName + "' already exists. Using existing tool.";
            }

            // 4. 创建动态工具
            DynamicGroovyTool newTool = new DynamicGroovyTool(toolName, description, groovyCode);

            // 5. 注册到注册表
            dynamicToolRegistry.registerTool(newTool);

            log.info("Successfully created dynamic tool: {}", toolName);

            return String.format(
                "Success: Tool '%s' created successfully. Description: %s. You can now use this tool.",
                toolName, description
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for tool creation", e);
            return "Error: " + e.getMessage();

        } catch (Exception e) {
            log.error("Failed to create tool", e);
            return "Error: Failed to create tool - " + e.getMessage();
        }
    }

    /**
     * 提取参数
     */
    private String extractParameter(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);

        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }

        return value.toString().trim();
    }

    /**
     * 验证参数
     */
    private void validateParameters(String toolName, String description, String groovyCode) {
        // 验证工具名称
        if (toolName.isEmpty()) {
            throw new IllegalArgumentException("tool_name cannot be empty");
        }

        if (!toolName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException(
                "tool_name must contain only lowercase letters, numbers, and underscores, " +
                "and must start with a letter or underscore"
            );
        }

        // 验证描述
        if (description.isEmpty()) {
            throw new IllegalArgumentException("description cannot be empty");
        }

        // 验证 Groovy 代码
        if (groovyCode.isEmpty()) {
            throw new IllegalArgumentException("groovy_code cannot be empty");
        }

        // 基本的代码安全检查（防止明显的危险操作）
        String lowerCode = groovyCode.toLowerCase();

        if (lowerCode.contains("system.exit") ||
            lowerCode.contains("runtime.getruntime") ||
            lowerCode.contains("processbuilder")) {
            throw new IllegalArgumentException(
                "Groovy code contains potentially dangerous operations (System.exit, Runtime, ProcessBuilder)"
            );
        }
    }

    /**
     * 获取工具元数据（用于注册到 Agent）
     */
    public ToolMetadata getMetadata() {
        return new ToolMetadata(TOOL_NAME, DESCRIPTION, PARAMETER_SCHEMA);
    }

    /**
     * 工具元数据
     */
    public static class ToolMetadata {
        public final String name;
        public final String description;
        public final String parameterSchema;

        public ToolMetadata(String name, String description, String parameterSchema) {
            this.name = name;
            this.description = description;
            this.parameterSchema = parameterSchema;
        }
    }
}
