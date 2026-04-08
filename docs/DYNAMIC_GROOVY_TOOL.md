# Tool 自举机制 - 动态 Groovy 工具

## 概述

Nanobot4J 实现了高阶的"Tool 自举"能力，允许在运行时动态执行大模型生成的代码。通过引入 Groovy 脚本引擎，突破了 Java 的编译限制，实现了真正的动态工具创建和执行。

## 核心概念

### 什么是 Tool 自举？

Tool 自举（Tool Bootstrapping）是指 Agent 能够在运行时动态创建和执行新的工具，而无需重新编译代码。这使得：

1. **大模型可以生成工具代码**：LLM 根据需求生成 Groovy 脚本
2. **运行时动态执行**：无需重启应用即可使用新工具
3. **快速迭代验证**：快速测试和调整工具逻辑
4. **扩展 Agent 能力**：动态增强 Agent 的功能

### 为什么选择 Groovy？

- ✅ **动态语言**：无需编译，运行时执行
- ✅ **Java 兼容**：可以直接调用 Java API
- ✅ **语法简洁**：比 Java 更简洁易读
- ✅ **成熟稳定**：Apache 项目，生产级质量
- ✅ **安全可控**：可以限制脚本权限

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                    Agent (LLM)                          │
│  "我需要一个计算器工具"                                   │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│              LLM 生成 Groovy 脚本                        │
│  def result = a + b                                     │
│  return "结果: ${result}"                                │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│            DynamicGroovyTool                            │
├─────────────────────────────────────────────────────────┤
│  - name: "calculator"                                   │
│  - description: "执行数学运算"                           │
│  - parameterSchema: {...}                               │
│  - scriptContent: "def result = a + b..."               │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│              GroovyShell.evaluate()                     │
├─────────────────────────────────────────────────────────┤
│  1. 创建 Binding 环境                                    │
│  2. 注入参数 (a=10, b=5)                                │
│  3. 执行脚本                                            │
│  4. 返回结果                                            │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│                  ToolResult                             │
│  content: "结果: 15"                                     │
│  success: true                                          │
└─────────────────────────────────────────────────────────┘
```

## 核心实现

### DynamicGroovyTool 类

```java
public class DynamicGroovyTool implements Tool {
    private final String name;
    private final String description;
    private final JsonNode parameterSchema;
    private final String scriptContent;
    private final GroovyShell groovyShell;

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        // 1. 创建 Binding 环境
        Binding binding = new Binding();

        // 2. 注入参数
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            binding.setVariable(entry.getKey(), entry.getValue());
        }

        // 3. 执行 Groovy 脚本
        Object result = groovyShell.evaluate(scriptContent, binding);

        // 4. 返回结果
        return ToolResult.success(convertResultToString(result));
    }
}
```

### 关键特性

1. **参数注入**：将 LLM 传递的参数注入到 Groovy 脚本环境
2. **工具类注入**：自动注入 `log` 和 `objectMapper` 等常用工具
3. **结果转换**：自动将执行结果转换为字符串或 JSON
4. **错误处理**：捕获脚本执行异常并返回友好的错误信息

## 使用示例

### 示例 1: 简单计算器

```java
String schemaJson = """
    {
        "type": "object",
        "properties": {
            "a": {"type": "number"},
            "b": {"type": "number"},
            "operation": {"type": "string"}
        },
        "required": ["a", "b", "operation"]
    }
    """;

String script = """
    def result
    switch (operation) {
        case 'add': result = a + b; break
        case 'subtract': result = a - b; break
        case 'multiply': result = a * b; break
        case 'divide': result = a / b; break
    }
    return "计算结果: ${a} ${operation} ${b} = ${result}"
    """;

DynamicGroovyTool calculator = new DynamicGroovyTool(
    "calculator",
    "执行基本的数学运算",
    schemaJson,
    script
);

// 执行
Map<String, Object> args = Map.of("a", 10, "b", 5, "operation", "add");
ToolResult result = calculator.execute(args);
// 输出: "计算结果: 10 add 5 = 15"
```

### 示例 2: 字符串处理

```java
String script = """
    switch (action) {
        case 'uppercase': return text.toUpperCase()
        case 'lowercase': return text.toLowerCase()
        case 'reverse': return text.reverse()
        case 'length': return "长度: ${text.length()}"
    }
    """;

DynamicGroovyTool stringProcessor = new DynamicGroovyTool(
    "string_processor",
    "处理字符串",
    schemaJson,
    script
);
```

### 示例 3: JSON 处理

```java
String script = """
    def person = [
        name: name,
        age: age,
        city: city,
        timestamp: new Date().toString(),
        isAdult: age >= 18
    ]
    return objectMapper.writeValueAsString(person)
    """;

DynamicGroovyTool jsonProcessor = new DynamicGroovyTool(
    "json_processor",
    "处理 JSON 数据",
    schemaJson,
    script
);
```

### 示例 4: 调用 Java API

```java
String script = """
    import java.net.URL

    def urlObj = new URL(url)
    return "协议: ${urlObj.protocol}, 主机: ${urlObj.host}"
    """;

DynamicGroovyTool urlParser = new DynamicGroovyTool(
    "url_parser",
    "解析 URL",
    schemaJson,
    script
);
```

## 与 Agent 集成

### 动态注册工具

```java
@Service
public class DynamicToolService {

    @Autowired
    private ToolRegistry toolRegistry;

    public void registerDynamicTool(String name, String description,
                                   String schemaJson, String script) {
        DynamicGroovyTool tool = new DynamicGroovyTool(
            name, description, schemaJson, script
        );

        toolRegistry.registerTool(tool);
        log.info("Registered dynamic tool: {}", name);
    }
}
```

### LLM 生成工具

```java
// 1. Agent 识别需要新工具
String userRequest = "我需要一个工具来计算两个数的最大公约数";

// 2. 调用 LLM 生成工具代码
String prompt = """
    请生成一个 Groovy 脚本来实现以下功能：
    %s

    要求：
    1. 参数通过变量直接访问（如 a, b）
    2. 返回字符串结果
    3. 包含错误处理
    """.formatted(userRequest);

String generatedScript = llmClient.generate(prompt);

// 3. 创建并注册工具
DynamicGroovyTool tool = new DynamicGroovyTool(
    "gcd_calculator",
    "计算最大公约数",
    schemaJson,
    generatedScript
);

toolRegistry.registerTool(tool);

// 4. Agent 可以立即使用新工具
```

## 安全考虑

### 1. 脚本沙箱

```java
// 限制脚本可访问的类和方法
CompilerConfiguration config = new CompilerConfiguration();
config.addCompilationCustomizers(
    new SecureASTCustomizer()
);

GroovyShell shell = new GroovyShell(config);
```

### 2. 超时控制

```java
// 设置脚本执行超时
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<Object> future = executor.submit(() -> {
    return groovyShell.evaluate(scriptContent, binding);
});

try {
    Object result = future.get(5, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    future.cancel(true);
    throw new ToolExecutionException("Script execution timeout");
}
```

### 3. 资源限制

```java
// 限制脚本可使用的内存和 CPU
// 使用 Java Security Manager 或容器化隔离
```

## 配置说明

### application.yml

```yaml
nanobot:
  dynamic-tools:
    enabled: true
    max-execution-time: 5000  # 最大执行时间（毫秒）
    enable-sandbox: true       # 启用沙箱模式
    allowed-packages:          # 允许访问的包
      - java.lang
      - java.util
      - java.time
```

## 最佳实践

### 1. 脚本模板

```groovy
// 推荐的脚本结构
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
    return "错误: ${e.message}"
}
```

### 2. 参数验证

```groovy
// 在脚本中验证参数
if (!(a instanceof Number) || !(b instanceof Number)) {
    return "错误: 参数必须是数字"
}
```

### 3. 日志记录

```groovy
// 使用注入的 log 对象
log.info("执行计算: {} + {}", a, b)
def result = a + b
log.info("计算结果: {}", result)
return result
```

### 4. 错误处理

```groovy
try {
    // 可能出错的代码
    def result = riskyOperation()
    return result
} catch (Exception e) {
    log.error("操作失败", e)
    return "错误: ${e.message}"
}
```

## 性能优化

### 1. 脚本缓存

```java
// 缓存编译后的脚本
private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();

public Object executeWithCache(String scriptContent, Binding binding) {
    Script script = scriptCache.computeIfAbsent(scriptContent,
        content -> groovyShell.parse(content)
    );
    script.setBinding(binding);
    return script.run();
}
```

### 2. 预编译

```java
// 在创建工具时预编译脚本
public DynamicGroovyTool(String name, String description,
                        JsonNode schema, String scriptContent) {
    this.compiledScript = groovyShell.parse(scriptContent);
}
```

## 故障排查

### 问题 1: 脚本执行失败

**检查：**
- 脚本语法是否正确
- 参数是否正确注入
- 是否有权限访问相关类

### 问题 2: 性能问题

**优化：**
- 启用脚本缓存
- 预编译脚本
- 减少脚本复杂度

### 问题 3: 内存泄漏

**解决：**
- 定期清理脚本缓存
- 限制并发执行数量
- 使用弱引用缓存

## 技术栈

- Apache Groovy 4.0.15
- GroovyShell（脚本执行引擎）
- Binding（变量绑定）
- Jackson（JSON 处理）

## 许可证

MIT License
