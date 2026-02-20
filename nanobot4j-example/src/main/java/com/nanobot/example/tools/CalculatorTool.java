package com.nanobot.example.tools;

import com.nanobot.starter.annotation.NanobotTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 计算器工具 - 演示如何使用 @NanobotTool 注解
 */
@Slf4j
@Component
public class CalculatorTool {

    @NanobotTool(
        name = "calculator",
        description = "执行基本的数学计算（加减乘除）",
        parameterSchema = """
            {
              "type": "object",
              "properties": {
                "operation": {
                  "type": "string",
                  "enum": ["add", "subtract", "multiply", "divide"],
                  "description": "运算类型"
                },
                "a": {
                  "type": "number",
                  "description": "第一个数字"
                },
                "b": {
                  "type": "number",
                  "description": "第二个数字"
                }
              },
              "required": ["operation", "a", "b"]
            }
            """
    )
    public String calculate(Map<String, Object> params) {
        String operation = (String) params.get("operation");
        double a = ((Number) params.get("a")).doubleValue();
        double b = ((Number) params.get("b")).doubleValue();

        log.info("Calculating: {} {} {}", a, operation, b);

        double result = switch (operation) {
            case "add" -> a + b;
            case "subtract" -> a - b;
            case "multiply" -> a * b;
            case "divide" -> {
                if (b == 0) {
                    throw new IllegalArgumentException("Cannot divide by zero");
                }
                yield a / b;
            }
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };

        return String.format("%.2f %s %.2f = %.2f", a, operation, b, result);
    }

    @NanobotTool(
        name = "weather",
        description = "查询指定城市的天气信息（模拟数据）",
        parameterSchema = """
            {
              "type": "object",
              "properties": {
                "city": {
                  "type": "string",
                  "description": "城市名称"
                }
              },
              "required": ["city"]
            }
            """
    )
    public String getWeather(Map<String, Object> params) {
        String city = (String) params.get("city");
        log.info("Getting weather for city: {}", city);

        // 模拟天气数据
        return String.format("城市：%s\n温度：25°C\n天气：晴朗\n湿度：60%%", city);
    }

    @NanobotTool(
        name = "time",
        description = "获取当前系统时间",
        parameterSchema = "{}"
    )
    public String getCurrentTime(Map<String, Object> params) {
        log.info("Getting current time");
        return "当前时间：" + java.time.LocalDateTime.now().toString();
    }
}
