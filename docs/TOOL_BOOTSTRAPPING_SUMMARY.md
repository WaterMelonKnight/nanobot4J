# Tool 自举机制 - 实现总结

## ✅ 已完成的工作

### 1. Maven 依赖

已在 `pom.xml` 中添加 Groovy 依赖：

```xml
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy-all</artifactId>
    <version>4.0.15</version>
    <type>pom</type>
</dependency>
```

### 2. 核心实现

#### DynamicGroovyTool 类
- 文件：`src/main/java/com/nanobot/tool/DynamicGroovyTool.java`
- 实现了 `Tool` 接口
- 核心功能：
  - ✅ 接收 Groovy 脚本字符串
  - ✅ 通过 GroovyShell 执行脚本
  - ✅ 参数注入到 Binding 环境
  - ✅ 自动注入 `log` 和 `objectMapper`
  - ✅ 结果自动转换（字符串/JSON）
  - ✅ 完善的错误处理

### 3. 核心代码

```java
@Override
public ToolResult execute(Map<String, Object> arguments) {
    Instant startTime = Instant.now();

    try {
        // 1. 创建 Binding 环境
        Binding binding = new Binding();

        // 2. 注入参数
        if (arguments != null) {
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                binding.setVariable(entry.getKey(), entry.getValue());
            }
        }

        // 3. 注入工具类
        binding.setVariable("log", log);
        binding.setVariable("objectMapper", objectMapper);

        // 4. 执行 Groovy 脚本
        Object result = groovyShell.evaluate(scriptContent, binding);

        // 5. 转换结果
        String resultString = convertResultToString(result);

        return new ToolResult(resultString, true, null, startTime, Instant.now());

    } catch (Exception e) {
        return new ToolResult(null, false,
            "Groovy script execution failed: " + e.getMessage(),
            startTime, Instant.now());
    }
}
```

### 4. 使用示例

#### 示例 1: 简单计算器
```java
String script = """
    def result = a + b
    return "结果: ${result}"
    """;

DynamicGroovyTool tool = new DynamicGroovyTool(
    "calculator", "计算器", schemaJson, script
);

Map<String, Object> args = Map.of("a", 10, "b", 5);
ToolResult result = tool.execute(args);
// 输出: "结果: 15"
```

#### 示例 2: 字符串处理
```java
String script = """
    return text.toUpperCase().reverse()
    """;
```

#### 示例 3: JSON 处理
```java
String script = """
    def person = [name: name, age: age]
    return objectMapper.writeValueAsString(person)
    """;
```

#### 示例 4: 调用 Java API
```java
String script = """
    import java.net.URL
    def urlObj = new URL(url)
    return "协议: ${urlObj.protocol}"
    """;
```

### 5. 完整示例代码

- 文件：`src/main/java/com/nanobot/example/DynamicGroovyToolExample.java`
- 包含 5 个完整示例：
  1. 简单计算器
  2. 字符串处理
  3. JSON 处理
  4. 复杂业务逻辑
  5. 调用 Java API

### 6. 文档

#### 完整技术文档
- 文件：`docs/DYNAMIC_GROOVY_TOOL.md`
- 内容：
  - 核心概念
  - 架构设计
  - 实现细节
  - 安全考虑
  - 性能优化
  - 故障排查

#### 快速开始指南
- 文件：`docs/QUICK_START_DYNAMIC_TOOL.md`
- 内容：
  - 5 分钟快速开始
  - 常用场景
  - LLM 集成
  - 常见问题

## 🎯 核心特性

### 1. 动态执行
- 运行时执行 Groovy 脚本
- 无需编译和重启
- 支持热更新

### 2. 参数注入
```java
// 参数自动注入到脚本环境
Map<String, Object> args = Map.of("a", 10, "b", 5);
// 脚本中可以直接使用 a 和 b
```

### 3. 工具类注入
```groovy
// 自动注入的工具类
log.info("日志记录")
objectMapper.writeValueAsString(data)
```

### 4. 结果转换
- 自动将结果转换为字符串
- 复杂对象自动转 JSON
- 支持自定义转换逻辑

### 5. 错误处理
```java
try {
    // 执行脚本
} catch (Exception e) {
    return ToolResult.failure("执行失败: " + e.getMessage());
}
```

## 🚀 使用场景

### 1. LLM 生成工具

```
用户: "我需要一个工具来计算最大公约数"
  ↓
LLM 生成 Groovy 脚本
  ↓
创建 DynamicGroovyTool
  ↓
注册到 ToolRegistry
  ↓
Agent 立即可用
```

### 2. 快速原型验证

```java
// 快速测试新功能
String script = "return 'Hello, ' + name";
DynamicGroovyTool tool = new DynamicGroovyTool(...);
ToolResult result = tool.execute(Map.of("name", "World"));
```

### 3. 动态业务逻辑

```java
// 根据配置动态加载业务规则
String script = loadScriptFromDatabase();
DynamicGroovyTool tool = new DynamicGroovyTool(...);
```

### 4. 扩展 Agent 能力

```java
// 运行时扩展 Agent
toolRegistry.registerTool(new DynamicGroovyTool(...));
// Agent 立即获得新能力
```

## 📊 架构优势

### 传统方式 vs Tool 自举

| 特性 | 传统方式 | Tool 自举 |
|-----|---------|----------|
| 添加新工具 | 编写 Java 代码 → 编译 → 重启 | 编写 Groovy 脚本 → 立即可用 |
| 开发周期 | 数小时 | 数分钟 |
| 灵活性 | 低 | 高 |
| LLM 生成 | 不支持 | 支持 |
| 热更新 | 不支持 | 支持 |

## 🔧 技术实现

### 核心组件

1. **GroovyShell**：脚本执行引擎
2. **Binding**：变量绑定环境
3. **ObjectMapper**：JSON 序列化
4. **Tool 接口**：统一工具接口

### 执行流程

```
1. 创建 DynamicGroovyTool
   ├─ name: "calculator"
   ├─ description: "计算器"
   ├─ parameterSchema: {...}
   └─ scriptContent: "def result = a + b..."

2. 调用 execute(args)
   ├─ 创建 Binding
   ├─ 注入参数 (a=10, b=5)
   ├─ 注入工具 (log, objectMapper)
   └─ 执行脚本

3. GroovyShell.evaluate()
   ├─ 解析脚本
   ├─ 执行代码
   └─ 返回结果

4. 结果转换
   ├─ String → 直接返回
   ├─ Object → JSON 序列化
   └─ null → "null"

5. 返回 ToolResult
   ├─ content: "结果: 15"
   ├─ success: true
   └─ executionTime: 10ms
```

## 💡 最佳实践

### 1. 脚本模板

```groovy
try {
    // 参数验证
    if (param == null) {
        return "错误: 参数不能为空"
    }

    // 业务逻辑
    log.info("开始处理: {}", param)
    def result = process(param)

    // 返回结果
    return "成功: ${result}"

} catch (Exception e) {
    log.error("处理失败", e)
    return "错误: ${e.message}"
}
```

### 2. 参数验证

```groovy
// 类型检查
if (!(a instanceof Number)) {
    return "错误: a 必须是数字"
}

// 范围检查
if (age < 0 || age > 150) {
    return "错误: 年龄超出范围"
}
```

### 3. 日志记录

```groovy
log.info("输入参数: a={}, b={}", a, b)
def result = a + b
log.info("计算结果: {}", result)
```

### 4. 错误处理

```groovy
try {
    def result = riskyOperation()
    return result
} catch (Exception e) {
    log.error("操作失败", e)
    return "错误: ${e.message}"
}
```

## ⚠️ 安全考虑

### 1. 脚本来源验证
- 只执行可信来源的脚本
- 对用户输入进行审查
- 使用白名单机制

### 2. 权限限制
- 限制可访问的类和方法
- 禁止执行系统命令
- 禁止访问敏感文件

### 3. 资源限制
- 设置执行超时（5-10 秒）
- 限制内存使用
- 限制并发执行数

### 4. 沙箱隔离
```java
// 使用 SecureASTCustomizer 限制脚本权限
CompilerConfiguration config = new CompilerConfiguration();
config.addCompilationCustomizers(new SecureASTCustomizer());
GroovyShell shell = new GroovyShell(config);
```

## 🎉 总结

已成功实现完整的 Tool 自举机制：

- ✅ 引入 Groovy 脚本引擎
- ✅ 实现 DynamicGroovyTool 类
- ✅ 支持参数注入和工具类注入
- ✅ 完善的错误处理和日志记录
- ✅ 自动结果转换（字符串/JSON）
- ✅ 完整的使用示例和文档

**核心价值：**
- 运行时动态创建工具
- LLM 可以生成代码并执行
- 无需编译和重启
- 快速扩展 Agent 能力

所有代码已完成，可直接使用！
