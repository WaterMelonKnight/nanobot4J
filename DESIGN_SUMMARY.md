# Nanobot4J 核心接口设计总结

## 📋 项目概览

Nanobot4J 是基于 Java 21 和 Spring Boot 3 的 Agent 框架，实现了 HKU Nanobot 的"思考-规划-执行"循环。

**技术栈：**
- Java 21 (Sealed Interface, Record, Pattern Matching, Virtual Threads)
- Spring Boot 3.2.2
- Spring AI 1.0.0-M4
- Maven

## 🎯 核心设计理念

### 1. 不逐行翻译，而是重新设计

我们没有简单地将 Python 代码翻译成 Java，而是：
- 充分利用 Java 21 的新特性（Sealed Interface、Record）
- 遵循 Java 生态的最佳实践（Spring IoC、接口优先）
- 采用同步阻塞模型（配合虚拟线程）而非 Python 的 asyncio

### 2. 接口优先的设计

所有核心组件都定义了清晰的接口：
- `Agent` - 定义智能体的生命周期
- `Memory` - 定义记忆管理
- `Tool` - 定义工具标准
- `LLMClient` - 定义 LLM 交互

这使得系统高度可扩展和可测试。

## 🏗️ 核心接口详解

### 1️⃣ Message（消息模型）

**文件：** `domain/Message.java`

```java
public sealed interface Message permits
    UserMessage, AssistantMessage, SystemMessage, ToolResultMessage
```

**设计亮点：**
- ✅ 使用 Sealed Interface 确保类型安全
- ✅ 四种消息类型覆盖完整的 LLM 交互生命周期
- ✅ 使用 Record 类型，不可变且简洁
- ✅ 每条消息都有唯一 ID 和时间戳

**为什么这样设计？**
- Sealed Interface 在编译期就能确保所有消息类型都被处理
- 配合 Java 21 的 Pattern Matching，代码更简洁
- 新增消息类型时，编译器会提示所有需要修改的地方

### 2️⃣ Tool（工具接口）

**文件：** `tool/Tool.java`, `tool/AbstractTool.java`

```java
public interface Tool {
    String getName();
    String getDescription();
    JsonNode getSchema();  // JSON Schema 格式
    ToolResult execute(Map<String, Object> arguments);
}
```

**设计亮点：**
- ✅ `getSchema()` 返回 JSON Schema，告诉 LLM 如何调用工具
- ✅ `execute()` 同步阻塞执行，适合虚拟线程
- ✅ `AbstractTool` 提供模板方法模式，子类只需实现 `doExecute()`
- ✅ `ToolRegistry` 使用 Spring 自动注册所有工具

**为什么这样设计？**
- JSON Schema 是 LLM Function Calling 的标准格式
- 同步阻塞简化了并发模型，虚拟线程提供高并发能力
- 模板方法模式统一了异常处理和结果封装

### 3️⃣ Memory（记忆接口）

**文件：** `memory/Memory.java`, `memory/InMemoryMemory.java`

```java
public interface Memory {
    void addMessage(Message message);
    List<Message> getMessages();
    List<Message> getContext();  // 智能上下文窗口管理
    void clear();
}
```

**设计亮点：**
- ✅ `getContext()` 自动管理上下文窗口，避免超出 token 限制
- ✅ 策略：保留所有系统消息 + 最近的对话
- ✅ 使用 `CopyOnWriteArrayList` 保证线程安全
- ✅ 接口设计支持多种实现（内存、数据库、Redis）

**为什么这样设计？**
- 上下文窗口管理是 LLM 应用的核心问题
- 系统消息定义 Agent 行为，必须保留
- 最近的消息最相关，优先保留

### 4️⃣ LLMClient（LLM 交互层）

**文件：** `llm/LLMClient.java`, `llm/SpringAILLMClient.java`

```java
public interface LLMClient {
    AssistantMessage chat(List<Message> messages);
    AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools);
}
```

**设计亮点：**
- ✅ 基于 Spring AI 的 `ChatClient`，不硬编码 HTTP 请求
- ✅ 支持工具调用（Function Calling）
- ✅ 同步阻塞风格，运行在虚拟线程中
- ✅ 抽象了不同 LLM 提供商的差异

**为什么这样设计？**
- Spring AI 提供了统一的抽象层
- 同步阻塞代码更易理解和调试
- 接口设计支持切换不同的 LLM 提供商

### 5️⃣ Agent（智能体接口）

**文件：** `agent/Agent.java`, `agent/BaseAgent.java`

```java
public interface Agent {
    void initialize();
    AgentResponse chat(String userMessage);
    AgentResponse run(int maxIterations);
    void reset();
}
```

**设计亮点：**
- ✅ `run()` 实现核心的"思考-规划-执行"循环
- ✅ 支持最大迭代次数限制，防止无限循环
- ✅ 返回 `AgentResponse` 包含完整的对话历史和状态
- ✅ 清晰的生命周期管理（初始化、运行、重置）

**核心执行流程：**
```java
while (iteration < maxIterations) {
    // 1. 思考：调用 LLM
    AssistantMessage response = llmClient.chatWithTools(context, tools);
    
    // 2. 检查：是否需要执行工具
    if (!response.hasToolCalls()) {
        return completed();  // 任务完成
    }
    
    // 3. 执行：运行工具并记录结果
    executeToolCalls(response.toolCalls());
    
    // 4. 循环：继续下一轮思考
}
```

## 🚀 Java 21 特性应用

### 1. Sealed Interface
```java
public sealed interface Message permits
    UserMessage, AssistantMessage, SystemMessage, ToolResultMessage
```
- 编译期类型安全
- 配合 Pattern Matching 使用

### 2. Record
```java
public record UserMessage(String id, String content, Instant timestamp) 
    implements Message
```
- 不可变数据类
- 自动生成 equals/hashCode/toString

### 3. Pattern Matching
```java
return switch (message) {
    case Message.UserMessage m -> new UserMessage(m.content());
    case Message.AssistantMessage m -> new AssistantMessage(m.content());
    // ...
};
```

### 4. Virtual Threads
- 所有阻塞操作都适合在虚拟线程中运行
- 无需 Reactive 编程的复杂性

## 🔧 Spring Boot 3 集成

### 1. 依赖注入
```java
@Component
public class ToolRegistry {
    public ToolRegistry(List<Tool> toolList) {
        // Spring 自动注入所有 Tool 实现
    }
}
```

### 2. Spring AI
```java
@Component
public class SpringAILLMClient implements LLMClient {
    private final ChatClient chatClient;
    // 使用 Spring AI 的 ChatClient
}
```

### 3. 配置管理
```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4
```

## 📦 项目结构

```
src/main/java/com/nanobot/
├── domain/              # 领域模型
│   ├── Message.java           (Sealed Interface + 4 Records)
│   ├── ToolCall.java          (Record)
│   └── AgentResponse.java     (Record)
│
├── agent/               # Agent 层
│   ├── Agent.java             (接口)
│   ├── BaseAgent.java         (核心实现)
│   └── AgentFactory.java      (工厂类)
│
├── memory/              # Memory 层
│   ├── Memory.java            (接口)
│   └── InMemoryMemory.java    (内存实现)
│
├── llm/                 # LLM 交互层
│   ├── LLMClient.java         (接口)
│   └── SpringAILLMClient.java (Spring AI 实现)
│
├── tool/                # Tool 层
│   ├── Tool.java              (接口)
│   ├── AbstractTool.java      (抽象基类)
│   ├── ToolRegistry.java      (工具注册表)
│   ├── ToolResult.java        (结果封装)
│   ├── ToolExecutionException.java
│   └── impl/
│       ├── CalculatorTool.java
│       └── TimeTool.java
│
├── config/              # 配置
│   ├── SpringAIConfig.java
│   └── AgentProperties.java
│
├── example/             # 示例
│   └── ExampleRunner.java
│
└── Nanobot4JApplication.java  # 主应用
```

## 🎨 设计模式应用

1. **策略模式** - Memory、LLMClient、Tool 都可以有多种实现
2. **模板方法模式** - AbstractTool 定义执行流程，子类实现具体逻辑
3. **工厂模式** - AgentFactory 简化 Agent 创建
4. **注册表模式** - ToolRegistry 管理所有工具

## 🆚 与 Python Nanobot 的对比

| 特性 | Python Nanobot | Nanobot4J |
|------|----------------|-----------|
| 类型系统 | 动态类型 | 静态类型 + Sealed Interface |
| 并发模型 | asyncio | 虚拟线程（同步阻塞） |
| 依赖注入 | 手动管理 | Spring IoC |
| LLM 调用 | 手写 HTTP | Spring AI ChatClient |
| 工具注册 | 装饰器 | Spring Component Scan |
| 消息模型 | 字典/类 | Sealed Interface + Record |
| 错误处理 | try/except | 异常 + Optional |

## ✨ 核心优势

1. **类型安全** - 编译期捕获错误，减少运行时问题
2. **易于扩展** - 接口优先设计，支持多种实现
3. **简单易懂** - 同步阻塞代码，无需理解 Reactive
4. **Spring 生态** - 充分利用 Spring Boot 的强大功能
5. **现代 Java** - 使用 Java 21 最新特性

## 🎓 学习价值

这个项目展示了如何：
- 将 Python 的动态设计转换为 Java 的静态设计
- 使用 Java 21 新特性构建现代应用
- 设计清晰的接口和抽象
- 集成 Spring AI 进行 LLM 调用
- 实现"思考-规划-执行"循环

## 📚 下一步

1. **添加更多工具** - 文件操作、网络请求、数据库查询等
2. **持久化 Memory** - 实现基于数据库的 Memory
3. **多 LLM 支持** - 添加 Anthropic、Google Gemini 等
4. **流式响应** - 支持 LLM 的流式输出
5. **Web 界面** - 添加 REST API 和前端界面

## 📄 许可证

MIT License
