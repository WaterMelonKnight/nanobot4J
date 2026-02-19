package com.nanobot.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nanobot.tool.AbstractTool;
import com.nanobot.tool.ToolExecutionException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 计算器工具 - 示例工具实现
 */
@Component
public class CalculatorTool extends AbstractTool {

    public CalculatorTool() {
        super("calculator", "Perform basic arithmetic operations (add, subtract, multiply, divide)");
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode schema = createBaseSchema();
        ObjectNode properties = (ObjectNode) schema.get("properties");

        // operation 参数
        ObjectNode operation = objectMapper.createObjectNode();
        operation.put("type", "string");
        operation.put("description", "The operation to perform");
        operation.set("enum", objectMapper.createArrayNode()
            .add("add").add("subtract").add("multiply").add("divide"));
        properties.set("operation", operation);

        // a 参数
        ObjectNode a = objectMapper.createObjectNode();
        a.put("type", "number");
        a.put("description", "First number");
        properties.set("a", a);

        // b 参数
        ObjectNode b = objectMapper.createObjectNode();
        b.put("type", "number");
        b.put("description", "Second number");
        properties.set("b", b);

        // required 字段
        schema.set("required", objectMapper.createArrayNode()
            .add("operation").add("a").add("b"));

        return schema;
    }

    @Override
    protected String doExecute(Map<String, Object> arguments) throws ToolExecutionException {
        String operation = (String) arguments.get("operation");
        double a = getNumber(arguments, "a");
        double b = getNumber(arguments, "b");

        double result = switch (operation) {
            case "add" -> a + b;
            case "subtract" -> a - b;
            case "multiply" -> a * b;
            case "divide" -> {
                if (b == 0) {
                    throw new ToolExecutionException("Division by zero");
                }
                yield a / b;
            }
            default -> throw new ToolExecutionException("Unknown operation: " + operation);
        };

        return String.format("%.2f %s %.2f = %.2f", a, operation, b, result);
    }

    private double getNumber(Map<String, Object> arguments, String key) throws ToolExecutionException {
        Object value = arguments.get(key);
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        throw new ToolExecutionException("Invalid number for parameter: " + key);
    }
}
