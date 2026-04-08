# Redis 企业级记忆模块使用指南

## 概述

本模块为 Nanobot4J 提供了基于 Redis 的企业级聊天记忆存储功能，支持多会话管理、自动过期、智能上下文控制等特性。

## 核心特性

- ✅ **多会话隔离**：每个 sessionId 独立存储，互不干扰
- ✅ **自动过期**：对话列表自动设置 TTL（默认 24 小时）
- ✅ **智能上下文管理**：基于 token 数量的智能消息检索
- ✅ **JSON 序列化**：完整支持 Message 类型的序列化/反序列化
- ✅ **线程安全**：基于 Spring Data Redis 的线程安全实现
- ✅ **高性能**：使用 Redis List 数据结构，支持高并发访问

## 快速开始

### 1. 启动 Redis

```bash
# 使用 Docker 启动 Redis
docker run -d --name redis -p 6379:6379 redis:latest

# 或使用本地 Redis
redis-server
```

### 2. 配置 application.yml

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: # 如果有密码请填写
      database: 0
      timeout: 3000ms

nanobot:
  redis:
    enabled: true
    ttl-hours: 24  # 会话过期时间（小时）
```

### 3. 注入并使用

```java
@Service
public class ChatService {

    private final ChatMemoryStore chatMemoryStore;

    public ChatService(ChatMemoryStore chatMemoryStore) {
        this.chatMemoryStore = chatMemoryStore;
    }

    public void chat(String sessionId, String userInput) {
        // 添加用户消息
        chatMemoryStore.addMessage(sessionId,
            new Message.UserMessage(userInput));

        // 获取历史上下文（最多 4096 tokens）
        List<Message> context = chatMemoryStore.getMessages(sessionId, 4096);

        // 调用 LLM 生成回复
        String response = callLLM(context);

        // 保存助手回复
        chatMemoryStore.addMessage(sessionId,
            new Message.AssistantMessage(response));
    }
}
```

## API 文档

### ChatMemoryStore 接口

#### 添加消息

```java
// 添加单条消息
void addMessage(String sessionId, Message message);

// 批量添加消息
void addMessages(String sessionId, List<Message> messages);
```

#### 检索消息

```java
// 获取所有消息
List<Message> getAllMessages(String sessionId);

// 基于 token 限制获取消息（智能截断）
List<Message> getMessages(String sessionId, int maxTokens);

// 获取最近 N 条消息
List<Message> getRecentMessages(String sessionId, int count);
```

#### 会话管理

```java
// 清空会话消息（保留会话）
void clear(String sessionId);

// 删除会话
void deleteSession(String sessionId);

// 检查会话是否存在
boolean exists(String sessionId);

// 获取消息数量
long getMessageCount(String sessionId);

// 刷新 TTL（延长过期时间）
void refreshTTL(String sessionId);
```

## 使用示例

### 示例 1: 基本对话流程

```java
String sessionId = "user-123";

// 用户发送消息
chatMemoryStore.addMessage(sessionId,
    new Message.UserMessage("你好，请介绍一下 Java"));

// 获取上下文
List<Message> context = chatMemoryStore.getMessages(sessionId, 4096);

// LLM 生成回复
String response = "Java 是一种面向对象的编程语言...";

// 保存回复
chatMemoryStore.addMessage(sessionId,
    new Message.AssistantMessage(response));
```

### 示例 2: 多会话管理

```java
// 会话 A
chatMemoryStore.addMessage("session-A",
    new Message.UserMessage("讨论 Java"));

// 会话 B
chatMemoryStore.addMessage("session-B",
    new Message.UserMessage("讨论 Python"));

// 两个会话完全隔离，互不影响
```

### 示例 3: 智能上下文控制

```java
// 获取最多 2000 tokens 的上下文
// 系统会自动从最新消息开始截断
List<Message> context = chatMemoryStore.getMessages(sessionId, 2000);

// 或者获取最近 10 条消息
List<Message> recent = chatMemoryStore.getRecentMessages(sessionId, 10);
```

### 示例 4: 会话生命周期管理

```java
// 检查会话是否存在
if (chatMemoryStore.exists(sessionId)) {
    // 刷新 TTL，延长过期时间
    chatMemoryStore.refreshTTL(sessionId);
}

// 清空会话消息
chatMemoryStore.clear(sessionId);

// 删除会话
chatMemoryStore.deleteSession(sessionId);
```

## Redis 数据结构

### Key 格式

```
chat:session:{sessionId}
```

### 数据类型

使用 Redis List 存储消息列表：
- 新消息添加到列表右侧（RPUSH）
- 最新消息在列表末尾
- 支持范围查询（LRANGE）

### TTL 设置

每次添加消息或刷新 TTL 时，自动设置过期时间为 24 小时。

## 配置说明

### application.yml 配置项

```yaml
spring:
  data:
    redis:
      host: localhost              # Redis 主机地址
      port: 6379                   # Redis 端口
      password:                    # Redis 密码（可选）
      database: 0                  # Redis 数据库索引
      timeout: 3000ms              # 连接超时时间
      lettuce:
        pool:
          max-active: 8            # 最大活跃连接数
          max-idle: 8              # 最大空闲连接数
          min-idle: 0              # 最小空闲连接数
          max-wait: -1ms           # 最大等待时间

nanobot:
  redis:
    enabled: true                  # 是否启用 Redis
    ttl-hours: 24                  # 会话过期时间（小时）
```

## 性能优化建议

1. **批量操作**：使用 `addMessages()` 批量添加消息，减少网络往返
2. **合理设置 maxTokens**：避免一次性加载过多消息
3. **定期清理**：对于不再使用的会话，及时调用 `deleteSession()`
4. **连接池配置**：根据并发量调整 Redis 连接池大小

## 故障排查

### 问题 1: 连接 Redis 失败

```
检查 Redis 是否启动：redis-cli ping
检查配置文件中的 host 和 port 是否正确
检查防火墙是否开放 6379 端口
```

### 问题 2: 序列化失败

```
确保 Message 类型已正确配置 Jackson 注解
检查是否注册了 JavaTimeModule
```

### 问题 3: TTL 未生效

```
检查 Redis 版本是否支持 EXPIRE 命令
使用 redis-cli TTL key 命令查看过期时间
```

## 运行示例

启用示例代码：

```yaml
nanobot:
  example:
    redis-memory:
      enabled: true
```

然后启动应用，查看控制台输出的示例演示。

## 技术栈

- Spring Boot 3.2.2
- Spring Data Redis
- Jackson (JSON 序列化)
- Lettuce (Redis 客户端)

## 许可证

MIT License
