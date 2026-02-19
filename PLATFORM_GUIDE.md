# Nanobot4J 平台使用指南

## 概述

Nanobot4J 现在是一个完整的后端平台，支持：
- ✅ 多机器人实例管理
- ✅ 会话持久化（数据库存储）
- ✅ 并发安全控制
- ✅ REST API 接口

## 架构概览

```
┌─────────────────────────────────────────────────────────┐
│                    REST API Layer                        │
│              (ChatController)                            │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  Service Layer                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │  AgentService                                     │  │
│  │  - 管理多个 Agent 实例                            │  │
│  │  - 会话创建和恢复                                 │  │
│  │  - 协调并发控制                                   │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  SessionManager                                   │  │
│  │  - 细粒度锁控制                                   │  │
│  │  - 防止同一会话并发请求                           │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                 Persistence Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ AgentConfig  │  │ ChatSession  │  │ ChatMessage  │ │
│  │ Repository   │  │ Repository   │  │ Repository   │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
              ┌──────────────┐
              │  H2/MySQL    │
              │  Database    │
              └──────────────┘
```

## 核心组件

### 1. AgentService

**职责：**
- 管理多个 Agent 实例（每个 Agent 可以有不同的配置）
- 处理会话创建和恢复
- 协调 SessionManager 进行并发控制
- 管理 Agent 和 Memory 的生命周期

**关键方法：**
```java
// 创建新会话
ChatSession createSession(String agentId, String userId)

// 处理聊天请求（带并发控制）
AgentResponse chat(String sessionId, String userMessage)

// 关闭会话并清理资源
void closeSession(String sessionId)
```

**缓存机制：**
- Agent 实例缓存：`agentId:sessionId -> Agent`
- Memory 实例缓存：`sessionId -> DatabaseMemory`

### 2. SessionManager

**职责：**
- 确保同一个 Session 不会同时处理多个请求
- 使用 ReentrantLock 实现细粒度锁控制
- 自动清理长时间未使用的锁

**并发控制示例：**
```java
public <T> T executeWithLock(String sessionId, SessionAction<T> action) 
    throws SessionLockedException {
    if (!tryLockSession(sessionId)) {
        throw new SessionLockedException("Session is locked");
    }
    try {
        return action.execute();
    } finally {
        unlockSession(sessionId);
    }
}
```

### 3. DatabaseMemory

**职责：**
- 将对话历史持久化到数据库
- 支持上下文窗口管理
- 使用事务确保数据一致性

**特点：**
- 所有消息都保存到 `chat_messages` 表
- 自动管理序列号（sequence_number）
- 支持 JSON 序列化工具调用

### 4. REST API

**端点：**
- `POST /api/v1/sessions` - 创建会话
- `POST /api/v1/chat/completions` - 发送消息
- `GET /api/v1/sessions/{sessionId}` - 获取会话信息
- `DELETE /api/v1/sessions/{sessionId}` - 关闭会话
- `GET /api/v1/health` - 健康检查

## 数据库设计

### 表结构

#### 1. agent_configs
存储 Agent 配置信息。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| agent_id | VARCHAR(64) | Agent 唯一标识 |
| name | VARCHAR(128) | Agent 名称 |
| system_prompt | TEXT | 系统提示词 |
| tool_ids | TEXT | 工具列表（JSON） |
| max_iterations | INT | 最大迭代次数 |
| context_window_size | INT | 上下文窗口大小 |
| enabled | BOOLEAN | 是否启用 |

#### 2. chat_sessions
存储会话信息。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| session_id | VARCHAR(64) | 会话唯一标识 |
| agent_id | VARCHAR(64) | 关联的 Agent |
| user_id | VARCHAR(128) | 用户 ID |
| active | BOOLEAN | 是否活跃 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 3. chat_messages
存储对话历史。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| message_id | VARCHAR(64) | 消息唯一标识 |
| session_id | VARCHAR(64) | 关联的会话 |
| role | VARCHAR(20) | 角色（user/assistant/system/tool） |
| content | TEXT | 消息内容 |
| tool_calls | TEXT | 工具调用（JSON） |
| sequence_number | INT | 序列号 |
| created_at | TIMESTAMP | 创建时间 |

## 使用示例

### 1. 启动应用

```bash
# 配置环境变量
export OPENAI_API_KEY=your-api-key

# 启动应用
mvn spring-boot:run
```

### 2. 创建会话

```bash
curl -X POST http://localhost:8080/api/v1/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "math-assistant",
    "userId": "user123"
  }'
```

响应：
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "agentId": "math-assistant",
  "userId": "user123",
  "createdAt": 1704067200000
}
```

### 3. 发送消息

```bash
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "message": "What is 123 + 456?"
  }'
```

响应：
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "content": "The result is 579.",
  "state": "COMPLETED",
  "iterationCount": 2,
  "timestamp": 1704067200000
}
```

### 4. 并发测试

```bash
# 同时发送两个请求到同一个 Session
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "xxx", "message": "Hello"}' &

curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "xxx", "message": "World"}' &
```

第二个请求会返回 409 Conflict：
```json
{
  "error": "session_locked",
  "message": "Session is currently processing another request"
}
```

## 并发安全机制

### 问题场景

如果同一个 Session 同时收到多个请求：
1. 对话历史可能混乱
2. Agent 状态不一致
3. 数据库并发冲突

### 解决方案

使用 `SessionManager` 实现细粒度锁：

```java
// AgentService.chat() 方法
public AgentResponse chat(String sessionId, String userMessage) throws Exception {
    return sessionManager.executeWithLock(sessionId, () -> {
        // 1. 获取会话
        // 2. 获取 Agent
        // 3. 执行对话
        // 4. 更新时间戳
        return response;
    });
}
```

### 锁的特点

- **细粒度**：每个 Session 独立的锁
- **公平锁**：先到先得
- **自动释放**：使用 try-finally 确保释放
- **非阻塞**：使用 tryLock，失败立即返回

## 查看数据库

### H2 Console

访问：http://localhost:8080/h2-console

配置：
- JDBC URL: `jdbc:h2:file:./data/nanobot4j`
- Username: `sa`
- Password: (留空)

### 查询示例

```sql
-- 查看所有 Agent 配置
SELECT * FROM agent_configs;

-- 查看活跃会话
SELECT * FROM chat_sessions WHERE active = TRUE;

-- 查看某个会话的对话历史
SELECT * FROM chat_messages 
WHERE session_id = 'xxx' 
ORDER BY sequence_number;
```

## 性能优化

### 1. 缓存策略

- Agent 实例缓存（避免重复创建）
- Memory 实例缓存（避免重复查询）
- 使用 ConcurrentHashMap 保证线程安全

### 2. 数据库优化

- 在 `session_id` 上创建索引
- 在 `created_at` 上创建索引
- 使用批量插入（saveAll）

### 3. 上下文窗口管理

- 保留所有系统消息
- 保留最近的 N 条消息
- 避免超出 LLM token 限制

## 扩展开发

### 添加新的 Agent

```sql
INSERT INTO agent_configs (
  agent_id, name, system_prompt, tool_ids,
  max_iterations, context_window_size, enabled,
  created_at, updated_at
) VALUES (
  'custom-agent',
  'Custom Agent',
  'Your system prompt',
  '["tool1", "tool2"]',
  10, 100, TRUE,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

### 添加新的工具

```java
@Component
public class MyTool extends AbstractTool {
    public MyTool() {
        super("my_tool", "Description");
    }
    
    @Override
    protected String doExecute(Map<String, Object> args) {
        return "result";
    }
    
    @Override
    public JsonNode getSchema() {
        return createBaseSchema();
    }
}
```

## 监控和日志

### 关键日志

```
INFO  - Created new session: sessionId=xxx, agentId=xxx
INFO  - Processing chat request: sessionId=xxx
WARN  - Session is locked: xxx
INFO  - Chat request completed: sessionId=xxx, iterations=2
INFO  - Closed session: xxx
```

### 监控指标

- 活跃会话数：`SessionManager.getActiveSessionCount()`
- 缓存大小：`agentCache.size()`, `memoryCache.size()`
- 数据库连接池状态

## 故障排查

### 问题：Session 被锁定

**现象：** 返回 409 Conflict

**原因：** 同一 Session 有其他请求正在处理

**解决：** 客户端实现重试机制或请求队列

### 问题：内存泄漏

**现象：** 内存持续增长

**原因：** Session 缓存未清理

**解决：** 定期调用 `closeSession()` 清理不活跃的会话

### 问题：数据库连接耗尽

**现象：** 无法获取数据库连接

**原因：** 连接池配置过小

**解决：** 调整 `spring.datasource.hikari.maximum-pool-size`

## 最佳实践

1. **及时关闭会话** - 不再使用的 Session 应及时关闭
2. **避免并发请求** - 客户端应实现请求队列
3. **合理设置上下文窗口** - 根据 LLM 限制调整
4. **监控缓存大小** - 防止内存泄漏
5. **使用连接池** - 优化数据库性能

## 下一步

1. 添加用户认证和授权
2. 实现流式响应（SSE）
3. 添加 WebSocket 支持
4. 实现分布式锁（Redis）
5. 添加监控和告警
