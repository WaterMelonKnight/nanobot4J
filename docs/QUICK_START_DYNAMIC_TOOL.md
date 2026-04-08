# Tool 自举 - 快速开始指南

## 🚀 5 分钟快速开始

### 1. 添加 Groovy 依赖

确保 `pom.xml` 包含 Groovy 依赖：

```xml
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy-all</artifactId>
    <version>4.0.15</version>
    <type>pom</type>
</dependency>
```

### 2. 创建动态工具

```java
// 定义参数 Schema
String schemaJson = """
    {
        "type": "object",
        "properties": {
            "a": {"type": "number"},
            "b": {"type": "number"}
        },
        "required": ["a", "b"]
    }
    """;

// 编写 Groovy 脚本
String script = """
    def result = a + b
    return "结果: ${result}"
    """;

// 创建工具
DynamicGroovyTool tool = new DynamicGroovyTool(
    "calculator",
    "计算两个数的和",
    schemaJson,
    script
);
```

### 3. 执行工具

```java
Map<String, Object> args = Map.of("a", 10, "b", 5);
ToolResult result = tool.execute(args);
System.out.println(result.content());  // 输出: "结果: 15"
```

## 📝 核心概念

### DynamicGroovyTool 构造函数

```java
public DynamicGroovyTool(
    String name,              // 工具名称
    String description,       // 工具描述
    JsonNode parameterSchema, // 参数 Schema（JSON Schema 格式）
    String scriptContent      // Groovy 脚本内容
)
```

### 参数注入机制

脚本中可以直接访问传入的参数：

```groovy
// 如果传入 {"a": 10, "b": 5}
// 脚本中可以直接使用 a 和 b
def sum = a + b
return sum
```

### 内置变量

脚本环境自动注入以下变量：

- `log` - SLF4J Logger 对象
- `objectMapper` - Jackson ObjectMapper 对象

```groovy
log.info("开始计算")
def result = [sum: a + b]
return objectMapper.writeValueAsString(result)
```

## 💡 常用场景

### 场景 1: 数学计算

```java
String script = """
    import java.math.BigDecimal

    def result = new BigDecimal(a).add(new BigDecimal(b))
    return result.toString()
    """;
```

### 场景 2: 字符串处理

```java
String script = """
    return text.toUpperCase().reverse()
    """;
```

### 场景 3: 日期处理

```java
String script = """
    import java.time.LocalDate
    import java.time.format.DateTimeFormatter

    def date = LocalDate.parse(dateStr)
    def formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    return date.format(formatter)
    """;
```

### 场景 4: HTTP 请求

```java
String script = """
    import java.net.http.*
    import java.net.URI

    def client = HttpClient.newHttpClient()
    def request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build()

    def response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
    """;
```

## 🔧 与 Agent 集成

### 注册到 ToolRegistry

```java
@Service
public class ToolService {

    @Autowired
    private ToolRegistry toolRegistry;

    public void registerCalculator() {
        DynamicGroovyTool tool = new DynamicGroovyTool(
            "calculator",
            "执行数学运算",
            schemaJson,
            script
        );

        toolRegistry.registerTool(tool);
    }
}
```

### Agent 使用动态工具

```java
// Agent 会自动发现并使用注册的工具
EnhancedAgent agent = new EnhancedAgent(...);
agent.chat("请帮我计算 10 + 5");
// Agent 会自动调用 calculator 工具
```

## 🎯 LLM 生成工具示例

### 提示词模板

```
请生成一个 Groovy 脚本来实现以下功能：
{用户需求}

要求：
1. 参数通过变量直接访问（如 a, b, text 等）
2. 返回字符串结果
3. 包含错误处理
4. 添加必要的日志

示例格式：
```groovy
try {
    // 参数验证
    if (param == null) {
        return "错误: 参数不能为空"
    }

    // 业务逻辑
    def result = doSomething(param)

    // 返回结果
    return "成功: ${result}"

} catch (Exception e) {
    log.error("执行失败", e)
    return "错误: ${e.message}"
}
```
```

### 完整流程

```java
// 1. 用户请求
String userRequest = "我需要一个工具来计算斐波那契数列";

// 2. 构建提示词
String prompt = buildPrompt(userRequest);

// 3. 调用 LLM 生成脚本
String generatedScript = llmClient.generate(prompt);

// 4. 创建工具
DynamicGroovyTool tool = new DynamicGroovyTool(
    "fibonacci",
    "计算斐波那契数列",
    schemaJson,
    generatedScript
);

// 5. 注册工具
toolRegistry.registerTool(tool);

// 6. 立即可用
```

## ⚠️ 安全注意事项

### 1. 限制脚本权限

```java
// 不要允许脚本访问敏感 API
// 如：System.exit(), Runtime.exec() 等
```

### 2. 设置执行超时

```java
// 防止脚本无限循环
// 建议设置 5-10 秒超时
```

### 3. 验证脚本来源

```java
// 只执行可信来源的脚本
// 对用户输入的脚本进行审查
```

## 📚 相关文档

- [完整技术文档](DYNAMIC_GROOVY_TOOL.md)
- [使用示例](../src/main/java/com/nanobot/example/DynamicGroovyToolExample.java)
- [Tool 接口文档](../src/main/java/com/nanobot/tool/Tool.java)

## ❓ 常见问题

**Q: 脚本可以访问哪些 Java 类？**
A: 默认可以访问所有 Java 标准库，建议限制敏感 API。

**Q: 如何调试脚本？**
A: 使用注入的 `log` 对象记录日志。

**Q: 脚本执行失败怎么办？**
A: 检查脚本语法、参数类型、异常信息。

**Q: 性能如何？**
A: 首次执行需要编译，后续可以缓存提高性能。

## 🎉 总结

DynamicGroovyTool 让你可以：

- ✅ 运行时动态创建工具
- ✅ LLM 生成代码并执行
- ✅ 快速扩展 Agent 能力
- ✅ 无需重启应用

开始使用吧！
