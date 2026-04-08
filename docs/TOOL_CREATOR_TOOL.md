# ToolCreatorTool - 让大模型自己创建工具

## 概述

ToolCreatorTool 是一个特殊的元工具（Meta-Tool），它让大模型能够在运行时动态创建新工具。这是 Tool 自举机制的最高级形态，实现了真正的"自我扩展"能力。

## 核心概念

### 什么是 ToolCreatorTool？

ToolCreatorTool 是一个内置工具，大模型可以调用它来创建新的工具。工作流程：

```
用户: "我需要计算斐波那契数列"
  ↓
大模型: "我没有这个工具，让我创建一个"
  ↓
大模型调用 ToolCreatorTool
  ├─ newToolName: "calculate_fibonacci"
  ├─ newToolDescription: "计算斐波那契数列"
  ├─ newToolParameterSchema: {...}
  └─ groovyScript: "def fib(n) { ... }"
  ↓
ToolCreatorTool 创建并注册新工具
  ↓
大模型: "工具创建成功，现在我来使用它"
  ↓
大模型调用 calculate_fibonacci
  ↓
返回结果给用户
```

## 核心实现

### ToolCreatorTool 类

```java
@Component
public class ToolCreatorTool extends AbstractTool {

    private final ToolRegistry toolRegistry;

    public ToolCreatorTool(ToolRegistry toolRegistry) {
        super("create_tool", "Create a new dynamic tool using Groovy script");
        this.toolRegistry = toolRegistry;
    }

    @Override
    protected String doExecute(Map<String, Object> arguments) {
        // 1. 提取参数
        String newToolName = getString(arguments, "newToolName");
        String newToolDescription = getString(arguments, "newToolDescription");
        String newToolParameterSchema = getString(arguments, "newToolParameterSchema");
        String groovyScript = getString(arguments, "groovyScript");

        // 2. 验证工具名称
        validateToolName(newToolName);

        // 3. 检查是否已存在
        if (toolRegistry.hasTool(newToolName)) {
            return "工具已存在";
        }

        // 4. 创建 DynamicGroovyTool
        DynamicGroovyTool newTool = new DynamicGroovyTool(
            newToolName, newToolDescription,
            newToolParameterSchema, groovyScript
        );

        // 5. 注册到 ToolRegistry
        toolRegistry.registerTool(newTool);

        // 6. 返回成功消息
        return "✅ 工具创建成功！你现在可以立即调用这个工具了！";
    }
}
```

### 参数说明

| 参数 | 类型 | 说明 |
|-----|------|------|
| newToolName | string | 工具名称（小写、下划线分隔） |
| newToolDescription | string | 工具描述 |
| newToolParameterSchema | string | JSON Schema 字符串 |
| groovyScript | string | Groovy 脚本代码 |

## 使用示例

### 示例 1: 创建斐波那契计算器

```json
{
  "newToolName": "calculate_fibonacci",
  "newToolDescription": "Calculate the nth Fibonacci number",
  "newToolParameterSchema": "{\"type\":\"object\",\"properties\":{\"n\":{\"type\":\"number\"}},\"required\":[\"n\"]}",
  "groovyScript": "def fib(int n) { if (n <= 1) return n; return fib(n-1) + fib(n-2) }; return \"Result: ${fib(n as int)}\""
}
```

### 示例 2: 创建字符串反转工具

```json
{
  "newToolName": "reverse_string",
  "newToolDescription": "Reverse a string",
  "newToolParameterSchema": "{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\"}},\"required\":[\"text\"]}",
  "groovyScript": "return text.reverse()"
}
```

### 示例 3: 创建温度转换工具

```json
{
  "newToolName": "convert_temperature",
  "newToolDescription": "Convert temperature between Celsius and Fahrenheit",
  "newToolParameterSchema": "{\"type\":\"object\",\"properties\":{\"value\":{\"type\":\"number\"},\"from\":{\"type\":\"string\"},\"to\":{\"type\":\"string\"}},\"required\":[\"value\",\"from\",\"to\"]}",
  "groovyScript": "if (from == 'celsius' && to == 'fahrenheit') { return value * 9 / 5 + 32 } else { return (value - 32) * 5 / 9 }"
}
```

## 大模型使用流程

### 完整对话示例

```
用户: 请帮我计算斐波那契数列的第 10 项

大模型思考:
- 我需要计算斐波那契数列
- 我没有这个工具
- 我可以使用 create_tool 创建一个

大模型调用 create_tool:
{
  "newToolName": "calculate_fibonacci",
  "newToolDescription": "Calculate Fibonacci number",
  "newToolParameterSchema": "...",
  "groovyScript": "..."
}

系统返回:
"✅ 工具创建成功！你现在可以立即调用这个工具了！"

大模型调用 calculate_fibonacci:
{
  "n": 10
}

系统返回:
"斐波那契数列第 10 项 = 55"

大模型回复用户:
"斐波那契数列的第 10 项是 55"
```

## 错误处理

### 1. 工具名称验证

```java
// 只允许小写字母、数字和下划线
if (!toolName.matches("^[a-z][a-z0-9_]*$")) {
    return "工具名称格式不正确";
}
```

**错误示例：**
- `MyTool` ❌ （包含大写字母）
- `123tool` ❌ （以数字开头）
- `my-tool` ❌ （包含连字符）

**正确示例：**
- `my_tool` ✅
- `calculate_fibonacci` ✅
- `parse_json` ✅

### 2. 重复工具检查

```java
if (toolRegistry.hasTool(newToolName)) {
    return "❌ 工具创建失败：工具已存在";
}
```

### 3. Schema 解析错误

```java
try {
    new DynamicGroovyTool(...);
} catch (Exception e) {
    return "❌ 工具创建失败：参数 Schema 解析错误";
}
```

### 4. 脚本语法错误

Groovy 脚本在执行时会捕获异常并返回友好的错误信息。

## 安全机制

### 1. 工具名称限制

- 只允许小写字母、数字和下划线
- 必须以字母开头
- 不能使用系统保留名称（如 `create_tool`）

### 2. 脚本沙箱

- 继承 DynamicGroovyTool 的安全机制
- 可以限制脚本访问的类和方法
- 可以设置执行超时

### 3. 权限控制

- 只有授权的 Agent 可以创建工具
- 可以限制创建工具的数量
- 可以审计工具创建记录

## 最佳实践

### 1. 工具命名规范

```
✅ 好的命名：
- calculate_fibonacci
- parse_json
- get_weather
- convert_temperature

❌ 不好的命名：
- fib
- tool1
- myTool
- do_something
```

### 2. 脚本模板

```groovy
try {
    // 参数验证
    if (param == null) {
        return "错误: 参数不能为空"
    }

    // 业务逻辑
    def result = process(param)

    // 返回结果
    return "成功: ${result}"

} catch (Exception e) {
    log.error("执行失败", e)
    return "错误: ${e.message}"
}
```

### 3. 参数 Schema 设计

```json
{
  "type": "object",
  "properties": {
    "param1": {
      "type": "string",
      "description": "清晰的参数描述"
    }
  },
  "required": ["param1"]
}
```

## 监控和管理

### 查看已创建的工具

```java
Collection<Tool> tools = toolRegistry.getAllTools();
tools.forEach(tool -> {
    System.out.println(tool.getName() + ": " + tool.getDescription());
});
```

### 删除工具

```java
toolRegistry.unregisterTool("tool_name");
```

### 工具使用统计

可以在 ToolRegistry 中添加统计功能：
- 工具调用次数
- 工具执行时间
- 工具成功率

## 技术栈

- Spring Boot 3.2.2
- Apache Groovy 4.0.15
- Jackson (JSON 处理)
- SLF4J (日志)

## 许可证

MIT License
