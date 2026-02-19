package com.nanobot.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Map;

/**
 * 抽象工具基类 - 提供通用的工具实现逻辑
 */
public abstract class AbstractTool implements Tool {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    private final String name;
    private final String description;

    protected AbstractTool(String name, String description) {
        this.name = name;
        this.description = description;
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
    public ToolResult execute(Map<String, Object> arguments) throws ToolExecutionException {
        Instant startTime = Instant.now();
        try {
            validateArguments(arguments);
            String result = doExecute(arguments);
            Instant endTime = Instant.now();
            return new ToolResult(result, true, null, startTime, endTime);
        } catch (Exception e) {
            Instant endTime = Instant.now();
            return new ToolResult(null, false, e.getMessage(), startTime, endTime);
        }
    }

    /**
     * 子类实现具体的执行逻辑
     */
    protected abstract String doExecute(Map<String, Object> arguments) throws ToolExecutionException;

    /**
     * 辅助方法：创建基础的 JSON Schema
     */
    protected ObjectNode createBaseSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", objectMapper.createObjectNode());
        schema.set("required", objectMapper.createArrayNode());
        return schema;
    }
}
