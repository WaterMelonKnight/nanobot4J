# Nanobot4J

基于 Java 21 和 Spring Boot 3 的 Agent 框架，灵感来自 HKU Nanobot。

## 核心设计思路

### 1. 架构概览

Nanobot4J 实现了经典的"思考-规划-执行"（Think-Plan-Execute）循环：

```
用户输入 → Agent → LLM 思考 → 工具调用 → 执行工具 → 返回结果 → LLM 继续思考 → 完成
```

### 2. 核心接口设计

#### 2.1 Message（消息模型）

使用 Java 21 的 **Sealed Interface** 确保类型安全：

```java
public sealed interface Message permits
    UserMessage, AssistantMessage, SystemMessage, ToolResultMessage
```

**设计亮点：**
- 使用 Record 类型，不可变且简洁
- 四种消息类型对应 LLM 交互的完整生命周期
- 每条消息都有唯一 ID 和时间戳

#### 2.2 Tool（工具接口）

```java
public interface Tool {
    String getName();
    String getDescription();
    JsonNode getSchema();  // JSON Schema 格式
    ToolResult execute(Map<String, Object> arguments);
}
```

**设计亮点：**
- `getSchema()` 返回 JSON Schema，告诉 LLM 如何调用工具
- `execute()` 同步阻塞执行，适合虚拟线程
- `AbstractTool` 提供通用实现，子类只需实现 `doExecute()`

#### 2.3 Memory（记忆接口）

```java
public interface Memory {
    void addMessage(Message message);
    List<Message> getMessages();
    List<Message> getContext();  // 智能上下文窗口管理
    void clear();
}
```

**设计亮点：**
- `getContext()` 自动管理上下文窗口，避免超出 token 限制
- 策略：保留所有系统消息 + 最近的对话
- 使用 `CopyOnWriteArrayList` 保证线程安全

#### 2.4 LLMClient（LLM 交互层）

```java
public interface LLMClient {
    AssistantMessage chat(List<Message> messages);
    AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools);
}
```

**设计亮点：**
- 基于 **Spring AI 的 ChatClient**，不硬编码 HTTP 请求
- 支持工具调用（Function Calling）
- 同步阻塞风格，运行在虚拟线程中

#### 2.5 Agent（智能体接口）

```java
public interface Agent {
    void initialize();
    AgentResponse chat(String userMessage);
    AgentResponse run(int maxIterations);
    void reset();
}
```

**设计亮点：**
- `run()` 实现核心的"思考-规划-执行"循环
- 支持最大迭代次数限制，防止无限循环
- 返回 `AgentResponse` 包含完整的对话历史和状态

### 3. 核心执行流程

`BaseAgent.run()` 的执行逻辑：

```java
while (iteration < maxIterations) {
    // 1. 思考：调用 LLM
    AssistantMessage response = llmClient.chatWithTools(context, tools);
    memory.addMessage(response);

    // 2. 检查：是否需要执行工具
    if (!response.hasToolCalls()) {
        return completed();  // 任务完成
    }

    // 3. 执行：运行工具并记录结果
    executeToolCalls(response.toolCalls());

    // 4. 循环：继续下一轮思考
}
```

### 4. Java 21 特性应用

- **Sealed Interface**：`Message` 使用 sealed 确保类型安全
- **Record**：所有数据类使用 Record，简洁且不可变
- **Pattern Matching**：在消息转换中使用 switch 表达式
- **Virtual Threads**：所有阻塞操作都适合在虚拟线程中运行

### 5. Spring Boot 3 集成

- **依赖注入**：`ToolRegistry` 自动注册所有 `@Component` 标注的工具
- **Spring AI**：使用 `ChatClient` 进行 LLM 调用
- **配置管理**：通过 `application.properties` 配置 API Key 和模型参数

## 快速开始

### 1. 配置

编辑 `src/main/resources/application.properties`：

```properties
spring.ai.openai.api-key=your-openai-api-key
spring.ai.openai.chat.options.model=gpt-4
```

### 2. 创建自定义工具

```java
@Component
public class MyTool extends AbstractTool {
    public MyTool() {
        super("my_tool", "Tool description");
    }

    @Override
    protected String doExecute(Map<String, Object> args) {
        // 实现工具逻辑
        return "result";
    }

    @Override
    public JsonNode getSchema() {
        // 定义参数 Schema
        return createBaseSchema();
    }
}
```

### 3. 使用 Agent

```java
@Autowired
private Memory memory;

@Autowired
private LLMClient llmClient;

@Autowired
private ToolRegistry toolRegistry;

public void runAgent() {
    Agent agent = new BaseAgent(
        "MyAgent",
        memory,
        llmClient,
        toolRegistry,
        "You are a helpful assistant."
    );

    agent.initialize();
    AgentResponse response = agent.chat("Calculate 123 + 456");
    System.out.println(response.content());
}
```

## 项目结构

```
src/main/java/com/nanobot/
├── domain/          # 领域模型（Message, ToolCall, AgentResponse）
├── agent/           # Agent 接口和实现
├── memory/          # Memory 接口和实现
├── llm/             # LLM 客户端接口和实现
├── tool/            # Tool 接口、抽象类和注册表
│   └── impl/        # 具体工具实现
└── config/          # Spring 配置
```

## 设计原则

1. **接口优先**：所有核心组件都定义接口，易于扩展和测试
2. **不可变性**：使用 Record 和 final 字段，减少状态管理复杂度
3. **类型安全**：使用 Sealed Interface 和泛型，编译期捕获错误
4. **同步阻塞**：简化并发模型，依赖虚拟线程提供高并发能力
5. **依赖注入**：充分利用 Spring 的 IoC 容器

## 与 Python Nanobot 的对比

| 特性 | Python Nanobot | Nanobot4J |
|------|----------------|-----------|
| 类型系统 | 动态类型 | 静态类型 + Sealed Interface |
| 并发模型 | asyncio | 虚拟线程（同步阻塞） |
| 依赖注入 | 手动管理 | Spring IoC |
| LLM 调用 | 手写 HTTP | Spring AI ChatClient |
| 工具注册 | 装饰器 | Spring Component Scan |

## 许可证

MIT License
