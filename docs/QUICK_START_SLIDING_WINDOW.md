# 滑动窗口 + 异步摘要 - 快速集成指南

## 🚀 5 分钟快速开始

### 1. 确保依赖已添加

检查 `pom.xml` 是否包含 Redis 依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. 启动 Redis

```bash
# 使用 Docker
docker run -d --name redis -p 6379:6379 redis:latest

# 或使用本地 Redis
redis-server
```

### 3. 配置 application.yml

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

nanobot:
  redis:
    enabled: true
  memory:
    window-size: 10
    summary-threshold: 10
```

### 4. 使用 EnhancedAgent

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

        agent.initialize();
        return agent.chat(userMessage);
    }
}
```

### 5. 测试

```bash
# 启动应用
mvn spring-boot:run

# 发送多条消息，观察日志
# 当消息数超过 10 条时，会自动触发异步摘要
```

## 📊 效果对比

| 对话轮数 | 传统方式 Token | 优化后 Token | 节省比例 |
|---------|---------------|-------------|---------|
| 10 轮   | 2000          | 2000        | 0%      |
| 20 轮   | 4000          | 1150        | 71%     |
| 50 轮   | 10000         | 1150        | 88%     |
| 100 轮  | 20000         | 1150        | 94%     |

## 🔧 核心组件说明

### EnhancedAgent
- 支持滑动窗口的增强版 Agent
- 自动触发异步摘要
- 智能上下文管理

### MemorySummarizer
- 使用 Java 21 虚拟线程
- 异步生成对话摘要
- 不阻塞主流程

### ChatMemoryStore
- 基于 Redis 的多会话存储
- 支持滑动窗口查询
- 自动 TTL 管理

### SummaryStore
- 存储会话摘要
- 快速读取
- 自动过期

## 💡 最佳实践

1. **合理设置窗口大小**：根据业务场景调整（5-15 条）
2. **监控 Token 消耗**：记录每次请求的 token 数
3. **使用本地模型生成摘要**：降低成本（推荐 Ollama + BitNet）
4. **定期清理过期会话**：避免 Redis 内存占用过高

## 📚 相关文档

- [完整技术文档](SLIDING_WINDOW_SUMMARY.md)
- [Redis 记忆模块指南](REDIS_MEMORY_GUIDE.md)
- [使用示例](../src/main/java/com/nanobot/example/EnhancedAgentExample.java)

## ❓ 常见问题

**Q: 摘要什么时候生成？**
A: 当消息数超过阈值（默认 10 条）且还没有摘要时，自动触发。

**Q: 摘要会阻塞对话吗？**
A: 不会，摘要在后台虚拟线程中异步生成。

**Q: 如何查看摘要内容？**
A: 使用 `summaryStore.getSummary(sessionId)` 获取。

**Q: 可以手动触发摘要吗？**
A: 可以，调用 `memorySummarizer.summarize(sessionId, messages)`。

## 🎯 下一步

- 根据业务场景调整配置参数
- 监控 Token 消耗和成本节省
- 优化摘要提示词以提高质量
- 考虑使用本地模型降低成本
