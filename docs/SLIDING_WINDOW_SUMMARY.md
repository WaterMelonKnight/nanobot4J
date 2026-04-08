# 滑动窗口 + 异步摘要机制

## 概述

为了优化大模型的 Token 成本，Nanobot4J 实现了"滑动窗口 + 异步摘要"机制。该机制通过智能的上下文管理，在保证对话质量的同时，大幅降低 Token 消耗。

## 核心原理

### 1. 滑动窗口（Sliding Window）

每次向大模型发送请求时，只携带最近的 N 条消息（默认 10 条），而不是全部历史记录。

**优势：**
- 减少上下文长度，降低 Token 消耗
- 提高响应速度
- 避免超出模型的上下文限制

### 2. 异步摘要（Async Summarization）

当对话历史超过阈值时，使用 Java 21 虚拟线程在后台异步生成摘要。

**工作流程：**
1. 检测到消息数 > 阈值（默认 10 条）
2. 启动虚拟线程，调用 LLM 生成摘要
3. 将摘要存储到 Redis
4. 下次对话时，将摘要作为 System Prompt 注入

**优势：**
- 不阻塞主流程
- 保留历史上下文的关键信息
- 摘要长度可控（默认 100 字）

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                    EnhancedAgent                        │
├─────────────────────────────────────────────────────────┤
│  1. 接收用户消息                                         │
│  2. 检查是否需要生成摘要                                 │
│  3. 构建上下文（摘要 + 滑动窗口）                        │
│  4. 调用 LLM                                            │
│  5. 保存响应                                            │
└─────────────────────────────────────────────────────────┘
           │                           │
           │                           │
           ▼                           ▼
┌──────────────────────┐    ┌──────────────────────┐
│  ChatMemoryStore     │    │  MemorySummarizer    │
│  (Redis)             │    │  (异步摘要服务)       │
├──────────────────────┤    ├──────────────────────┤
│ - 存储完整对话历史    │    │ - 使用虚拟线程        │
│ - 支持滑动窗口查询    │    │ - 调用 LLM 生成摘要   │
│ - 自动 TTL 管理      │    │ - 保存到 SummaryStore │
└──────────────────────┘    └──────────────────────┘
           │                           │
           │                           ▼
           │                ┌──────────────────────┐
           │                │   SummaryStore       │
           │                │   (Redis)            │
           │                ├──────────────────────┤
           │                │ - 存储会话摘要        │
           │                │ - 自动 TTL 管理      │
           │                └──────────────────────┘
           │                           │
           └───────────────────────────┘
```

## 核心组件

### 1. EnhancedAgent

增强版 Agent，支持滑动窗口和异步摘要。

**关键方法：**
- `buildContext()` - 构建上下文（摘要 + 滑动窗口）
- `checkAndTriggerSummarization()` - 检查并触发摘要生成

### 2. MemorySummarizer

对话摘要服务，负责生成和管理摘要。

**关键方法：**
- `summarizeAsync()` - 异步生成摘要（使用虚拟线程）
- `summarize()` - 同步生成摘要
- `shouldSummarize()` - 判断是否需要生成摘要

### 3. ChatMemoryStore

聊天记忆存储，支持多会话管理。

**关键方法：**
- `getRecentMessages()` - 获取最近 N 条消息（滑动窗口）
- `getAllMessages()` - 获取完整历史
- `getMessageCount()` - 获取消息总数

### 4. SummaryStore

摘要存储，管理会话摘要。

**关键方法：**
- `saveSummary()` - 保存摘要
- `getSummary()` - 获取摘要
- `hasSummary()` - 检查是否有摘要

## 使用示例

### 基本使用

```java
@Service
public class ChatService {

    @Autowired
    private ChatMemoryStore chatMemoryStore;

    @Autowired
    private SummaryStore summaryStore;

    @Autowired
    private MemorySummarizer memorySummarizer;

    @Autowired
    private LLMClient llmClient;

    @Autowired
    private ToolRegistry toolRegistry;

    public AgentResponse chat(String sessionId, String userMessage) {
        // 创建 EnhancedAgent
        EnhancedAgent agent = new EnhancedAgent(
            "MyAgent",
            sessionId,
            chatMemoryStore,
            summaryStore,
            memorySummarizer,
            llmClient,
            toolRegistry,
            "你是一个友好的 AI 助手。"
        );

        // 初始化
        agent.initialize();

        // 对话
        return agent.chat(userMessage);
    }
}
```

### 自定义窗口大小

```java
// 创建窗口大小为 5 的 Agent
EnhancedAgent agent = new EnhancedAgent(
    "MyAgent",
    sessionId,
    chatMemoryStore,
    summaryStore,
    memorySummarizer,
    llmClient,
    toolRegistry,
    "你是一个技术助手。",
    5  // 窗口大小
);
```

### 手动触发摘要

```java
// 获取历史消息
List<Message> messages = chatMemoryStore.getAllMessages(sessionId);

// 同步生成摘要
String summary = memorySummarizer.summarize(sessionId, messages);

// 或异步生成摘要
CompletableFuture<String> future = memorySummarizer.summarizeAsync(sessionId, messages);
future.thenAccept(summary -> {
    System.out.println("摘要生成完成: " + summary);
});
```

## 配置说明

### application.yml

```yaml
nanobot:
  memory:
    window-size: 10          # 滑动窗口大小（最近 N 条消息）
    summary-threshold: 10    # 触发摘要的消息数阈值
    summary-max-length: 100  # 摘要最大字数
```

## 工作流程

### 第一次对话（消息数 ≤ 10）

```
User: 你好
  ↓
[检查] 消息数 = 2，不需要摘要
  ↓
[构建上下文] 最近 10 条消息（实际只有 2 条）
  ↓
[调用 LLM] 发送上下文
  ↓
AI: 你好！有什么可以帮助你的吗？
```

### 第 11 次对话（触发摘要）

```
User: 第 11 个问题
  ↓
[检查] 消息数 = 22，需要生成摘要
  ↓
[异步摘要] 启动虚拟线程，生成前 10 条消息的摘要
  │
  ├─ 后台线程：调用 LLM 生成摘要
  │  ↓
  │  保存摘要到 Redis
  │
[构建上下文] 最近 10 条消息
  ↓
[调用 LLM] 发送上下文
  ↓
AI: 回答第 11 个问题
```

### 第 12 次对话（使用摘要）

```
User: 第 12 个问题
  ↓
[检查] 已有摘要，不需要重新生成
  ↓
[构建上下文]
  ├─ 摘要（作为 System Prompt）
  └─ 最近 10 条消息
  ↓
[调用 LLM] 发送上下文（包含摘要）
  ↓
AI: 基于历史摘要和最近消息回答
```

## Token 节省效果

### 传统方式（无优化）

```
第 20 次对话：
- 历史消息：40 条
- 平均每条：100 tokens
- 总计：4000 tokens
```

### 滑动窗口 + 摘要

```
第 20 次对话：
- 摘要：1 条（约 150 tokens）
- 最近消息：10 条（约 1000 tokens）
- 总计：1150 tokens
- 节省：71.25%
```

## 性能优化

### 1. 使用虚拟线程

```java
CompletableFuture.supplyAsync(() -> {
    return summarize(sessionId, messages);
}, Thread.ofVirtual().factory());  // Java 21 虚拟线程
```

**优势：**
- 轻量级，可创建大量线程
- 不阻塞主流程
- 自动管理线程池

### 2. Redis 缓存

- 摘要存储在 Redis，快速读取
- 自动 TTL 管理，避免内存泄漏
- 支持分布式部署

### 3. 智能触发

- 只在必要时生成摘要
- 避免重复生成
- 可配置阈值

## 最佳实践

### 1. 合理设置窗口大小

```yaml
# 短对话场景
window-size: 5

# 长对话场景
window-size: 15

# 技术支持场景
window-size: 10
```

### 2. 调整摘要阈值

```yaml
# 频繁生成摘要
summary-threshold: 8

# 减少摘要频率
summary-threshold: 15
```

### 3. 监控 Token 消耗

```java
// 记录每次请求的 token 数
log.info("Context size: {} messages, estimated tokens: {}",
    context.size(), context.size() * 100);
```

## 故障排查

### 问题 1: 摘要未生成

**检查：**
- 消息数是否超过阈值
- LLM 服务是否正常
- Redis 连接是否正常

### 问题 2: 上下文不连贯

**解决：**
- 增加窗口大小
- 降低摘要阈值
- 优化摘要提示词

### 问题 3: 性能问题

**优化：**
- 使用本地模型（Ollama）生成摘要
- 调整 Redis 连接池大小
- 增加虚拟线程数量

## 技术栈

- Java 21（虚拟线程）
- Spring Boot 3.2.2
- Spring Data Redis
- CompletableFuture（异步编程）

## 许可证

MIT License
