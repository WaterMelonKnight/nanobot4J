package com.nanobot.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * 动态 Groovy 工具 - 支持运行时执行 Groovy 脚本
 *
 * 核心特性：
 * 1. 运行时动态执行 Groovy 脚本
 * 2. 支持参数注入到脚本环境
 * 3. 实现 Tool 接口，可被 Agent 调用
 * 4. 支持 Tool 自举（大模型生成代码并执行）
 * 5. 完善的错误处理和安全机制
 *
 * 使用场景：
 * - 大模型生成工具代码并动态执行
 * - 运行时扩展 Agent 能力
 * - 快速原型验证
 * - 动态业务逻辑
 */
public class DynamicGroovyTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(DynamicGroovyTool.class);

    private final String name;
    private final String description;
    private final JsonNode parameterSchema;
    private final String scriptContent;
    private final GroovyShell groovyShell;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param name 工具名称
     * @param description 工具描述
     * @param parameterSchema 参数 Schema（JSON Schema 格式）
     * @param scriptContent Groovy 脚本内容
     */
    public DynamicGroovyTool(
            String name,
            String description,
            JsonNode parameterSchema,
            String scriptContent) {
        this.name = name;
        this.description = description;
        this.parameterSchema = parameterSchema;
        this.scriptContent = scriptContent;
        this.groovyShell = new GroovyShell();
        this.objectMapper = new ObjectMapper();

        log.info("Created DynamicGroovyTool: {}", name);
    }

    /**
     * 便捷构造函数 - 接收 JSON 字符串形式的 Schema
     *
     * @param name 工具名称
     * @param description 工具描述
     * @param parameterSchemaJson 参数 Schema（JSON 字符串）
     * @param scriptContent Groovy 脚本内容
     */
    public DynamicGroovyTool(
            String name,
            String description,
            String parameterSchemaJson,
            String scriptContent) {
        this.name = name;
        this.description = description;
        this.scriptContent = scriptContent;
        this.groovyShell = new GroovyShell();
        this.objectMapper = new ObjectMapper();

        try {
            this.parameterSchema = objectMapper.readTree(parameterSchemaJson);
        } catch (Exception e) {
            log.error("Failed to parse parameter schema JSON", e);
            throw new IllegalArgumentException("Invalid parameter schema JSON", e);
        }

        log.info("Created DynamicGroovyTool: ", name);
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
    public JsonNode getSchema() {
        return parameterSchema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) throws ToolExecutionException {
        Instant startTime = Instant.now();

        log.debug("Executing DynamicGroovyTool '{}' with arguments: {}", name, arguments);

        try {
            // 1. 创建 Binding 环境
            Binding binding = new Binding();

            // 2. 将参数注入到 Binding 中
            if (arguments != null) {
                for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                    binding.setVariable(entry.getKey(), entry.getValue());
                    log.debug("  Injected variable: {} = {}", entry.getKey(), entry.getValue());
                }
            }

            // 3. 注入常用工具类（可选）
            binding.setVariable("log", log);
            binding.setVariable("objectMapper", objectMapper);

            // 4. 执行 Groovy 脚本（使用带 Binding 的 GroovyShell）
            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(scriptContent);

            // 5. 将结果转换为字符串
            String resultString = convertResultToString(result);

            Instant endTime = Instant.now();

            log.info("DynamicGroovyTool '{}' executed successfully: {}", name, resultString);

            return new ToolResult(
                    resultString,
                    true,
                    null,
                    startTime,
                    endTime
            );

        } catch (Exception e) {
            Instant endTime = Instant.now();
            String errorMessage = "Groovy script execution failed: " + e.getMessage();

            log.error("DynamicGroovyTool '{}' execution failed", name, e);

            return new ToolResult(
                    null,
                    false,
                    errorMessage,
                    startTime,
                    endTime
            );
        }
    }

    /**
     * 将执行结果转换为字符串
     */
    private String convertResultToString(Object result) {
        if (result == null) {
            return "null";
        }

        if (result instanceof String) {
            return (String) result;
        }

        // 对于复杂对象，尝试转换为 JSON
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            // 如果 JSON 序列化失败，使用 toString()
            return result.toString();
        }
    }

    /**
     * 获取脚本内容（用于调试）
     */
    public String getScriptContent() {
        return scriptContent;
    }

    @Override
    public String toString() {
        return "DynamicGroovyTool{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
