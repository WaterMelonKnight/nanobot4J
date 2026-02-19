package com.nanobot.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nanobot.tool.AbstractTool;
import com.nanobot.tool.ToolExecutionException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 时间工具 - 获取当前时间
 */
@Component
public class TimeTool extends AbstractTool {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TimeTool() {
        super("get_current_time", "Get the current date and time");
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode schema = createBaseSchema();
        ObjectNode properties = (ObjectNode) schema.get("properties");

        // format 参数（可选）
        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "string");
        format.put("description", "Time format (default: yyyy-MM-dd HH:mm:ss)");
        properties.set("format", format);

        // 没有必需参数
        schema.set("required", objectMapper.createArrayNode());

        return schema;
    }

    @Override
    protected String doExecute(Map<String, Object> arguments) throws ToolExecutionException {
        String format = (String) arguments.getOrDefault("format", "yyyy-MM-dd HH:mm:ss");

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDateTime now = LocalDateTime.now();
            return now.format(formatter);
        } catch (Exception e) {
            throw new ToolExecutionException("Invalid time format: " + format, e);
        }
    }
}
