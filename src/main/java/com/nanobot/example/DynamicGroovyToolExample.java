package com.nanobot.example;

import com.nanobot.tool.DynamicGroovyTool;
import com.nanobot.tool.ToolExecutionException;
import com.nanobot.tool.ToolResult;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * DynamicGroovyTool 使用示例
 *
 * 演示如何使用 Groovy 脚本实现动态工具
 */
@Component
@ConditionalOnProperty(name = "nanobot.example.dynamic-groovy-tool.enabled", havingValue = "true")
public class DynamicGroovyToolExample implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n========== DynamicGroovyTool 示例 ==========\n");

        // 示例 1: 简单计算器
        example1_SimpleCalculator();

        // 示例 2: 字符串处理
        example2_StringProcessor();

        // 示例 3: JSON 处理
        example3_JsonProcessor();

        // 示例 4: 复杂业务逻辑
        example4_ComplexLogic();

        // 示例 5: 调用 Java API
        example5_JavaAPI();

        System.out.println("\n========== 示例执行完成 ==========\n");
    }

    /**
     * 示例 1: 简单计算器
     */
    private void example1_SimpleCalculator() throws ToolExecutionException {
        System.out.println("=== 示例 1: 简单计算器 ===");

        String schemaJson = """
                {
                    "type": "object",
                    "properties": {
                        "a": {"type": "number", "description": "第一个数字"},
                        "b": {"type": "number", "description": "第二个数字"},
                        "operation": {"type": "string", "description": "操作符: add, subtract, multiply, divide"}
                    },
                    "required": ["a", "b", "operation"]
                }
                """;

        String script = """
                def result
                switch (operation) {
                    case 'add':
                        result = a + b
                        break
                    case 'subtract':
                        result = a - b
                        break
                    case 'multiply':
                        result = a * b
                        break
                    case 'divide':
                        result = a / b
                        break
                    default:
                        result = "Unknown operation"
                }
                return "计算结果: ${a} ${operation} ${b} = ${result}"
                """;

        DynamicGroovyTool calculator = new DynamicGroovyTool(
                "calculator",
                "执行基本的数学运算",
                schemaJson,
                script
        );

        // 测试加法
        Map<String, Object> args = new HashMap<>();
        args.put("a", 10);
        args.put("b", 5);
        args.put("operation", "add");

        ToolResult result = calculator.execute(args);
        System.out.println("结果: " + result.content());
        System.out.println();
    }

    /**
     * 示例 2: 字符串处理
     */
    private void example2_StringProcessor() throws ToolExecutionException {
        System.out.println("=== 示例 2: 字符串处理 ===");

        String schemaJson = """
                {
                    "type": "object",
                    "properties": {
                        "text": {"type": "string", "description": "要处理的文本"},
                        "action": {"type": "string", "description": "操作: uppercase, lowercase, reverse, length"}
                    },
                    "required": ["text", "action"]
                }
                """;

        String script = """
                def result
                switch (action) {
                    case 'uppercase':
                        result = text.toUpperCase()
                        break
                    case 'lowercase':
                        result = text.toLowerCase()
                        break
                    case 'reverse':
                        result = text.reverse()
                        break
                    case 'length':
                        result = "长度: ${text.length()}"
                        break
                    default:
                        result = "Unknown action"
                }
                return result
                """;

        DynamicGroovyTool stringProcessor = new DynamicGroovyTool(
                "string_processor",
                "处理字符串",
                schemaJson,
                script
        );

        // 测试转大写
        Map<String, Object> args = new HashMap<>();
        args.put("text", "Hello World");
        args.put("action", "uppercase");

        ToolResult result = stringProcessor.execute(args);
        System.out.println("结果: " + result.content());
        System.out.println();
    }

    /**
     * 示例 3: JSON 处理
     */
    private void example3_JsonProcessor() throws ToolExecutionException {
        System.out.println("=== 示例 3: JSON 处理 ===");

        String schemaJson = """
                {
                    "type": "object",
                    "properties": {
                        "name": {"type": "string"},
                        "age": {"type": "number"},
                        "city": {"type": "string"}
                    },
                    "required": ["name", "age", "city"]
                }
                """;

        String script = """
                // 构建 JSON 对象
                def person = [
                    name: name,
                    age: age,
                    city: city,
                    timestamp: new Date().toString(),
                    isAdult: age >= 18
                ]

                // 使用注入的 objectMapper 转换为 JSON
                return objectMapper.writeValueAsString(person)
                """;

        DynamicGroovyTool jsonProcessor = new DynamicGroovyTool(
                "json_processor",
                "处理 JSON 数据",
                schemaJson,
                script
        );

        Map<String, Object> args = new HashMap<>();
        args.put("name", "张三");
        args.put("age", 25);
        args.put("city", "北京");

        ToolResult result = jsonProcessor.execute(args);
        System.out.println("结果: " + result.content());
        System.out.println();
    }

    /**
     * 示例 4: 复杂业务逻辑
     */
    private void example4_ComplexLogic() throws ToolExecutionException {
        System.out.println("=== 示例 4: 复杂业务逻辑 ===");

        String schemaJson = """
                {
                    "type": "object",
                    "properties": {
                        "score": {"type": "number", "description": "分数 (0-100)"}
                    },
                    "required": ["score"]
                }
                """;

        String script = """
                // 根据分数判断等级
                def grade
                def comment

                if (score >= 90) {
                    grade = 'A'
                    comment = '优秀'
                } else if (score >= 80) {
                    grade = 'B'
                    comment = '良好'
                } else if (score >= 70) {
                    grade = 'C'
                    comment = '中等'
                } else if (score >= 60) {
                    grade = 'D'
                    comment = '及格'
                } else {
                    grade = 'F'
                    comment = '不及格'
                }

                return "分数: ${score}, 等级: ${grade}, 评价: ${comment}"
                """;

        DynamicGroovyTool gradeCalculator = new DynamicGroovyTool(
                "grade_calculator",
                "计算成绩等级",
                schemaJson,
                script
        );

        Map<String, Object> args = new HashMap<>();
        args.put("score", 85);

        ToolResult result = gradeCalculator.execute(args);
        System.out.println("结果: " + result.content());
        System.out.println();
    }

    /**
     * 示例 5: 调用 Java API
     */
    private void example5_JavaAPI() throws ToolExecutionException {
        System.out.println("=== 示例 5: 调用 Java API ===");

        String schemaJson = """
                {
                    "type": "object",
                    "properties": {
                        "url": {"type": "string", "description": "URL 地址"}
                    },
                    "required": ["url"]
                }
                """;

        String script = """
                import java.net.URL

                try {
                    def urlObj = new URL(url)
                    def info = [
                        protocol: urlObj.protocol,
                        host: urlObj.host,
                        port: urlObj.port,
                        path: urlObj.path
                    ]

                    return "URL 解析结果:\\n" +
                           "  协议: ${info.protocol}\\n" +
                           "  主机: ${info.host}\\n" +
                           "  端口: ${info.port}\\n" +
                           "  路径: ${info.path}"
                } catch (Exception e) {
                    return "URL 解析失败: ${e.message}"
                }
                """;

        DynamicGroovyTool urlParser = new DynamicGroovyTool(
                "url_parser",
                "解析 URL",
                schemaJson,
                script
        );

        Map<String, Object> args = new HashMap<>();
        args.put("url", "https://api.example.com:8080/v1/users");

        ToolResult result = urlParser.execute(args);
        System.out.println("结果:\n" + result.content());
        System.out.println();
    }
}
