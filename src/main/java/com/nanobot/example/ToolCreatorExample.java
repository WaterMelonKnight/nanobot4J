package com.nanobot.example;

import com.nanobot.tool.ToolExecutionException;
import com.nanobot.tool.ToolRegistry;
import com.nanobot.tool.ToolResult;
import com.nanobot.tool.impl.ToolCreatorTool;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ToolCreatorTool 使用示例
 *
 * 演示大模型如何动态创建新工具
 */
@Component
@ConditionalOnProperty(name = "nanobot.example.tool-creator.enabled", havingValue = "true")
public class ToolCreatorExample implements CommandLineRunner {

    private final ToolCreatorTool toolCreatorTool;
    private final ToolRegistry toolRegistry;

    public ToolCreatorExample(ToolCreatorTool toolCreatorTool, ToolRegistry toolRegistry) {
        this.toolCreatorTool = toolCreatorTool;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n========== ToolCreatorTool 示例 ==========\n");

        // 示例 1: 创建斐波那契计算器
        example1_CreateFibonacciTool();

        // 示例 2: 创建字符串反转工具
        example2_CreateStringReverseTool();

        // 示例 3: 创建温度转换工具
        example3_CreateTemperatureConverter();

        // 示例 4: 错误处理 - 重复创建
        example4_DuplicateToolName();

        // 示例 5: 错误处理 - 无效的工具名称
        example5_InvalidToolName();

        System.out.println("\n========== 示例执行完成 ==========\n");
    }

    /**
     * 示例 1: 创建斐波那契计算器
     */
    private void example1_CreateFibonacciTool() throws ToolExecutionException {
        System.out.println("=== 示例 1: 创建斐波那契计算器 ===");

        Map<String, Object> args = new HashMap<>();
        args.put("newToolName", "calculate_fibonacci");
        args.put("newToolDescription", "Calculate the nth Fibonacci number");
        args.put("newToolParameterSchema", """
            {
                "type": "object",
                "properties": {
                    "n": {
                        "type": "number",
                        "description": "The position in Fibonacci sequence"
                    }
                },
                "required": ["n"]
            }
            """);
        args.put("groovyScript", """
            def fib(int n) {
                if (n <= 1) return n
                return fib(n - 1) + fib(n - 2)
            }

            try {
                def num = n as int
                if (num < 0) {
                    return "错误: n 必须是非负整数"
                }
                def result = fib(num)
                return "斐波那契数列第 ${num} 项 = ${result}"
            } catch (Exception e) {
                return "错误: ${e.message}"
            }
            """);

        ToolResult result = toolCreatorTool.execute(args);
        System.out.println(result.content());
        System.out.println();

        // 测试新创建的工具
        if (result.success() && toolRegistry.hasTool("calculate_fibonacci")) {
            System.out.println("测试新工具:");
            var fibTool = toolRegistry.getTool("calculate_fibonacci").get();
            var testResult = fibTool.execute(Map.of("n", 10));
            System.out.println("  " + testResult.content());
        }
        System.out.println();
    }

    /**
     * 示例 2: 创建字符串反转工具
     */
    private void example2_CreateStringReverseTool() throws ToolExecutionException {
        System.out.println("=== 示例 2: 创建字符串反转工具 ===");

        Map<String, Object> args = new HashMap<>();
        args.put("newToolName", "reverse_string");
        args.put("newToolDescription", "Reverse a string");
        args.put("newToolParameterSchema", """
            {
                "type": "object",
                "properties": {
                    "text": {
                        "type": "string",
                        "description": "The text to reverse"
                    }
                },
                "required": ["text"]
            }
            """);
        args.put("groovyScript", """
            return text.reverse()
            """);

        ToolResult result = toolCreatorTool.execute(args);
        System.out.println(result.content());
        System.out.println();

        // 测试新工具
        if (result.success() && toolRegistry.hasTool("reverse_string")) {
            System.out.println("测试新工具:");
            var tool = toolRegistry.getTool("reverse_string").get();
            var testResult = tool.execute(Map.of("text", "Hello World"));
            System.out.println("  " + testResult.content());
        }
        System.out.println();
    }

    /**
     * 示例 3: 创建温度转换工具
     */
    private void example3_CreateTemperatureConverter() throws ToolExecutionException {
        System.out.println("=== 示例 3: 创建温度转换工具 ===");

        Map<String, Object> args = new HashMap<>();
        args.put("newToolName", "convert_temperature");
        args.put("newToolDescription", "Convert temperature between Celsius and Fahrenheit");
        args.put("newToolParameterSchema", """
            {
                "type": "object",
                "properties": {
                    "value": {
                        "type": "number",
                        "description": "Temperature value"
                    },
                    "from": {
                        "type": "string",
                        "description": "Source unit: celsius or fahrenheit"
                    },
                    "to": {
                        "type": "string",
                        "description": "Target unit: celsius or fahrenheit"
                    }
                },
                "required": ["value", "from", "to"]
            }
            """);
        args.put("groovyScript", """
            def result
            if (from == 'celsius' && to == 'fahrenheit') {
                result = value * 9 / 5 + 32
            } else if (from == 'fahrenheit' && to == 'celsius') {
                result = (value - 32) * 5 / 9
            } else if (from == to) {
                result = value
            } else {
                return "错误: 不支持的转换 ${from} -> ${to}"
            }
            return "${value}°${from.toUpperCase()[0]} = ${result}°${to.toUpperCase()[0]}"
            """);

        ToolResult result = toolCreatorTool.execute(args);
        System.out.println(result.content());
        System.out.println();

        // 测试新工具
        if (result.success() && toolRegistry.hasTool("convert_temperature")) {
            System.out.println("测试新工具:");
            var tool = toolRegistry.getTool("convert_temperature").get();
            var testResult = tool.execute(Map.of(
                "value", 100,
                "from", "celsius",
                "to", "fahrenheit"
            ));
            System.out.println("  " + testResult.content());
        }
        System.out.println();
    }

    /**
     * 示例 4: 错误处理 - 重复创建
     */
    private void example4_DuplicateToolName() throws ToolExecutionException {
        System.out.println("=== 示例 4: 错误处理 - 重复创建 ===");

        // 尝试创建已存在的工具
        Map<String, Object> args = new HashMap<>();
        args.put("newToolName", "calculate_fibonacci");  // 已存在
        args.put("newToolDescription", "Duplicate tool");
        args.put("newToolParameterSchema", "{\"type\":\"object\"}");
        args.put("groovyScript", "return 'test'");

        ToolResult result = toolCreatorTool.execute(args);
        System.out.println(result.content());
        System.out.println();
    }

    /**
     * 示例 5: 错误处理 - 无效的工具名称
     */
    private void example5_InvalidToolName() throws ToolExecutionException {
        System.out.println("=== 示例 5: 错误处理 - 无效的工具名称 ===");

        // 使用大写字母（不允许）
        Map<String, Object> args = new HashMap<>();
        args.put("newToolName", "MyTool");  // 无效：包含大写字母
        args.put("newToolDescription", "Invalid tool name");
        args.put("newToolParameterSchema", "{\"type\":\"object\"}");
        args.put("groovyScript", "return 'test'");

        ToolResult result = toolCreatorTool.execute(args);
        System.out.println(result.content());
        System.out.println();
    }
}
