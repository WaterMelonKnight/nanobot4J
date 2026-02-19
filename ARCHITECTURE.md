# Nanobot4J 架构设计文档

## 1. 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                         用户层                               │
│                    (User Interface)                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Agent 层                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  BaseAgent (思考-规划-执行循环)                       │  │
│  │  - initialize()                                       │  │
│  │  - chat(userMessage)                                  │  │
│  │  - run(maxIterations)                                 │  │
│  └──────────────────────────────────────────────────────┘  │
└───────┬──────────────────┬──────────────────┬──────────────┘
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Memory 层   │  │   LLM 层     │  │   Tool 层    │
├──────────────┤  ├──────────────┤  ├──────────────┤
│ InMemory     │  │ SpringAI     │  │ ToolRegistry │
│ Memory       │  │ LLMClient    │  │              │
│              │  │              │  │ Calculator   │
│ - addMessage │  │ - chat()     │  │ TimeTool     │
│ - getContext │  │ - chatWith   │  │ ...          │
│ - clear()    │  │   Tools()    │  │              │
└──────────────┘  └──────┬───────┘  └──────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │  Spring AI   │
                  │  ChatClient  │
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │   OpenAI     │
                  │   API        │
                  └──────────────┘
```

## 2. 核心执行流程

### 2.1 思考-规划-执行循环

```
开始
  │
  ▼
┌─────────────────┐
│ 用户输入消息     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 添加到 Memory    │
└────────┬────────┘
         │
         ▼
    ┌────────┐
    │ 迭代循环│ ◄──────────────┐
    └────┬───┘                │
         │                    │
         ▼                    │
┌─────────────────┐           │
│ 1. 思考阶段      │           │
│ - 获取上下文     │           │
│ - 调用 LLM      │           │
└────────┬────────┘           │
         │                    │
         ▼                    │
┌─────────────────┐           │
│ 2. 判断阶段      │           │
│ 是否有工具调用？ │           │
└────┬───────┬────┘           │
     │       │                │
    否       是               │
     │       │                │
     │       ▼                │
     │  ┌─────────────────┐  │
     │  │ 3. 执行阶段      │  │
     │  │ - 查找工具       │  │
     │  │ - 执行工具       │  │
     │  │ - 记录结果       │  │
     │  └────────┬────────┘  │
     │           │            │
     │           └────────────┘
     │
     ▼
┌─────────────────┐
│ 返回最终响应     │
└─────────────────┘
```

## 3. 数据流

### 3.1 消息流转

```
UserMessage
    │
    ▼
Memory.addMessage()
    │
    ▼
Memory.getContext()
    │
    ▼
LLMClient.chatWithTools(messages, tools)
    │
    ▼
Spring AI ChatClient
    │
    ▼
OpenAI API
    │
    ▼
AssistantMessage (可能包含 ToolCall)
    │
    ▼
Memory.addMessage()
    │
    ├─→ 如果有 ToolCall
    │       │
    │       ▼
    │   ToolRegistry.getTool()
    │       │
    │       ▼
    │   Tool.execute()
    │       │
    │       ▼
    │   ToolResultMessage
    │       │
    │       ▼
    │   Memory.addMessage()
    │       │
    │       └─→ 继续循环
    │
    └─→ 如果没有 ToolCall
            │
            ▼
        返回 AgentResponse
```

## 4. 类图

### 4.1 Message 类层次

```
<<sealed interface>>
Message
    │
    ├─→ UserMessage (record)
    │   - id: String
    │   - content: String
    │   - timestamp: Instant
    │
    ├─→ AssistantMessage (record)
    │   - id: String
    │   - content: String
    │   - toolCalls: List<ToolCall>
    │   - timestamp: Instant
    │
    ├─→ SystemMessage (record)
    │   - id: String
    │   - content: String
    │   - timestamp: Instant
    │
    └─→ ToolResultMessage (record)
        - id: String
        - toolCallId: String
        - toolName: String
        - result: String
        - success: boolean
        - timestamp: Instant
```

### 4.2 Tool 类层次

```
<<interface>>
Tool
    │
    ├─→ AbstractTool (abstract)
    │       │
    │       ├─→ CalculatorTool
    │       ├─→ TimeTool
    │       └─→ ... (自定义工具)
    │
    └─→ ToolRegistry
        - tools: Map<String, Tool>
        + registerTool(Tool)
        + getTool(String): Optional<Tool>
```

## 5. 关键设计决策

### 5.1 为什么使用 Sealed Interface？

- **类型安全**：编译期确保所有消息类型都被处理
- **模式匹配**：配合 Java 21 的 switch 表达式，代码更简洁
- **可维护性**：新增消息类型时，编译器会提示所有需要修改的地方

### 5.2 为什么使用同步阻塞而非 Reactive？

- **简单性**：同步代码更易理解和调试
- **虚拟线程**：Java 21 的虚拟线程提供高并发能力，无需 Reactive
- **LLM 特性**：LLM 调用本身是顺序的，异步收益有限

### 5.3 为什么使用 Spring AI？

- **抽象层**：统一不同 LLM 提供商的 API
- **工具调用**：内置 Function Calling 支持
- **Spring 集成**：与 Spring Boot 生态无缝集成

### 5.4 Memory 的上下文窗口管理

策略：
1. 保留所有 SystemMessage（系统提示词）
2. 保留最近的 N 条对话消息
3. 当消息超过限制时，丢弃中间的历史消息

原因：
- 系统提示词定义 Agent 行为，必须保留
- 最近的消息最相关，优先保留
- 避免超出 LLM 的 token 限制

## 6. 扩展点

### 6.1 自定义工具

```java
@Component
public class MyCustomTool extends AbstractTool {
    public MyCustomTool() {
        super("my_tool", "Tool description");
    }

    @Override
    protected String doExecute(Map<String, Object> args) {
        // 实现逻辑
        return "result";
    }

    @Override
    public JsonNode getSchema() {
        // 定义参数 Schema
        return createBaseSchema();
    }
}
```

### 6.2 自定义 Memory 实现

```java
@Component
public class DatabaseMemory implements Memory {
    // 实现基于数据库的持久化存储
}
```

### 6.3 自定义 LLMClient

```java
@Component
public class AnthropicLLMClient implements LLMClient {
    // 实现 Anthropic Claude API 调用
}
```

## 7. 性能考虑

### 7.1 并发模型

- 使用虚拟线程处理多个 Agent 实例
- 每个 Agent 内部是单线程的（简化状态管理）
- Memory 使用 CopyOnWriteArrayList 保证线程安全

### 7.2 内存管理

- Message 使用 Record，不可变且内存高效
- Memory 有上下文窗口限制，避免无限增长
- 工具执行结果及时记录，避免重复计算

### 7.3 LLM 调用优化

- 批量处理工具调用（一次 LLM 调用可返回多个工具调用）
- 上下文窗口管理减少 token 使用
- 可配置最大迭代次数，避免无限循环

## 8. 安全考虑

### 8.1 工具执行安全

- 工具执行有异常捕获，不会导致 Agent 崩溃
- 可以为工具添加权限检查
- 工具执行结果记录，便于审计

### 8.2 输入验证

- Tool 接口提供 `validateArguments()` 方法
- LLM 返回的工具调用参数需要验证
- 防止注入攻击（SQL、命令注入等）

## 9. 测试策略

### 9.1 单元测试

- 每个 Tool 独立测试
- Memory 实现测试
- Message 转换逻辑测试

### 9.2 集成测试

- Agent 完整流程测试
- LLM 调用 Mock 测试
- 工具调用链路测试

### 9.3 端到端测试

- 真实 LLM 调用测试
- 多轮对话测试
- 错误恢复测试
