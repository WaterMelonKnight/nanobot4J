# Nanobot4J

> 一个面向生产环境的分布式 Java Agent 基础设施框架，将大模型推理能力与企业级微服务治理深度融合。

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

---

## 项目简介

Nanobot4J 是一个基于 Java 21 构建的**分布式 AI Agent 基础设施**。它以 ReAct（Reasoning + Acting）为核心范式，通过仿 XXL-JOB 的去中心化工具治理架构，将任意 Spring Boot 微服务无侵入地接入 Agent 工具体系。框架内置多级记忆压缩、动态工具自举和工业级防死循环机制，可直接承载企业级 AI 应用的生产负载。

---

## 核心架构与特性

### 1. 无侵入式微服务接入

基于自研 `nanobot4j-spring-boot-starter`，业务服务只需引入依赖并在方法上标注 `@NanobotTool`，框架即可在启动时自动扫描、提取工具元数据（名称、描述、参数 Schema），并通过心跳机制注册到 Admin 控制台。**业务代码零改造，接入成本极低。**

```java
@NanobotTool(name = "query_order", description = "根据订单号查询订单状态")
public String queryOrder(String orderId) {
    return orderService.getStatus(orderId);
}
```

### 2. 去中心化工具治理

参考 XXL-JOB 的执行器注册模型，实现了完整的**双向服务治理**：

- **Client → Admin 心跳注册**：各微服务实例定期上报自身地址与工具列表，Admin 维护在线实例注册表。
- **Admin → Client 动态 RPC**：Agent 执行工具调用时，Admin 通过 HTTP 反向调用对应 Client 实例，实现运行时动态路由。
- **服务发现与健康检测**：Admin 自动剔除心跳超时的实例，保证工具调用的高可用性。

```
[Agent Admin]  ←── 心跳注册 ───  [微服务 A (含 @NanobotTool)]
     │                            [微服务 B (含 @NanobotTool)]
     └──── HTTP RPC 调用 ────────► [目标工具实例]
```

### 3. 高并发流式引擎

结合 **Java 21 Virtual Threads（虚拟线程）** 与 **SSE（Server-Sent Events）**，彻底解决 Agent 推理链路中的 I/O 长时阻塞问题：

- 每个 SSE 长连接由一个虚拟线程承载，百万级并发下内存开销极低。
- ReAct 循环的每个中间状态（`THINKING` / `TOOL_CALL` / `TOOL_RESULT`）实时推送到前端，实现**思维链（CoT）可视化**。
- 前端无需轮询，延迟低至毫秒级。

```
LLM 推理中... → [THINKING 事件] → [TOOL_CALL 事件] → [TOOL_RESULT 事件] → [FINAL_ANSWER 事件]
                    ↑ 实时 SSE 推流，全程可观测
```

### 4. 多级成本压缩记忆

针对大模型 Token 成本问题，设计了三级记忆压缩架构：

| 层级 | 机制 | 实现 |
|------|------|------|
| L1 短期记忆 | 滑动窗口（最近 10 条） | `RedisChatMemoryStore` |
| L2 长期摘要 | 异步 LLM 摘要压缩 | `MemorySummarizer` + Virtual Thread |
| L3 持久化 | Redis List + 24h TTL | `RedisSummaryStore` |

当会话消息超过阈值时，后台虚拟线程自动调用 LLM 对历史消息生成摘要，并以 `System Message` 形式注入后续上下文，**在保留语义连贯性的同时大幅削减 Token 消耗**。

### 5. 动态工具自举（Self-Synthesizing）

突破静态工具边界的核心创新。当 Agent 遭遇现有工具无法覆盖的需求时，无需人工介入：

1. LLM 调用内置的 `create_tool`，传入工具名称、描述和 Groovy 脚本。
2. `ToolCreatorTool` 在运行时实例化 `DynamicGroovyTool` 并注册到工具注册表。
3. Agent 立即调用新工具完成任务，**整个过程在单次对话内自动完成**。

**安全沙箱保障**：Groovy 脚本在独立线程中执行，配备 5 秒超时熔断、危险 API 拦截（`System.exit`、`Runtime`、`ProcessBuilder`）和全异常捕获，主线程绝对安全。

```
用户: "帮我计算斐波那契数列第 30 项"
Agent: [发现无此工具] → create_tool(fibonacci, groovy_code) → fibonacci(30) → FINAL_ANSWER: 832040
```

### 6. 工业级防御机制

ReAct 引擎内置多层防御，确保生产环境稳定性：

- **强制 `<thinking>` 反思缓冲**：System Prompt 强制要求 LLM 在每次工具调用前输出思考过程，尤其在上一步发生错误时进行反思，有效抑制代码幻觉。
- **调用指纹校验（防死循环）**：引擎追踪每次工具调用的 `(工具名 + 参数)` 指纹，连续 2 次相同错误时自动注入系统警告，强制 LLM 切换策略。
- **最大步数熔断器**：ReAct 循环设置 `max_steps = 15` 硬上限，超出后强制退出并推送 `ERROR` 事件，防止无限循环消耗资源。

---

## 模块说明

```
nanobot4J/
├── nanobot4j-core/                 # 核心 SDK（无 Spring 依赖）
│   ├── agent/Agent.java            # Agent 顶层接口
│   ├── llm/                        # LLM 客户端抽象（LLMClient, Message）
│   ├── tool/                       # 工具接口（Tool, ToolResult, ToolDefinition）
│   └── memory/                     # 基础记忆接口（Memory, InMemoryMemory）
│
├── nanobot4j-spring-boot-starter/  # 自动装配 Starter
│   ├── @NanobotTool 注解扫描        # 自动提取工具元数据
│   ├── 心跳注册客户端               # 定时向 Admin 上报实例信息
│   └── 工具 HTTP 执行端点           # 接收 Admin 的 RPC 调用
│
├── nanobot4j-admin/                # Agent 控制台（核心服务）
│   ├── service/StreamingGenericReActAgent  # 工业级 ReAct 引擎（Phase 3）
│   ├── service/GenericReActAgent           # 非流式 ReAct 引擎
│   ├── service/InstanceRegistry            # 服务实例注册表
│   ├── service/RemoteToolExecutor          # 远程工具 RPC 执行器
│   ├── memory/                             # 多级记忆子系统（Phase 1）
│   │   ├── RedisChatMemoryStore            # Redis 会话存储
│   │   ├── RedisSummaryStore               # Redis 摘要存储
│   │   └── MemorySummarizer                # 滑动窗口 + 异步摘要
│   ├── tool/                               # 动态工具子系统（Phase 2）
│   │   ├── DynamicGroovyTool               # Groovy 沙箱执行器
│   │   ├── DynamicToolRegistry             # 动态工具注册表
│   │   └── ToolCreatorTool                 # LLM 自我编程工具
│   └── controller/                         # REST + SSE 接口层
│
└── nanobot4j-example/              # 示例工具服务
    └── 演示如何通过 Starter 接入 Admin
```

| 模块 | 定位 | 关键依赖 |
|------|------|---------|
| `nanobot4j-core` | 纯 Java 抽象层，无框架耦合 | 无 |
| `nanobot4j-spring-boot-starter` | Spring Boot 自动装配 | core |
| `nanobot4j-admin` | Agent 大脑，承载所有推理逻辑 | core, starter, Redis, Groovy |
| `nanobot4j-example` | 工具服务示例 | starter |

---

## 快速开始

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 21+ |
| Maven | 3.8+ |
| Redis | 6.0+（Admin 记忆功能需要） |

### 1. 克隆并编译

```bash
git clone https://github.com/your-org/nanobot4J.git
cd nanobot4J
mvn clean package -DskipTests -f pom-parent.xml
```

### 2. 启动 Admin 控制台

```bash
# 配置 LLM 和 Redis（编辑 application.yml）
vim nanobot4j-admin/src/main/resources/application.yml

# 启动
java -jar nanobot4j-admin/target/nanobot4j-admin-1.0.0-SNAPSHOT.jar
```

Admin 默认监听 `http://localhost:8080`，关键接口：

| 接口 | 说明 |
|------|------|
| `POST /api/agent/stream/chat` | SSE 流式对话（支持多轮 sessionId） |
| `GET  /api/agent/stream/stats` | 活跃连接数监控 |
| `POST /api/registry/register` | Client 心跳注册 |
| `GET  /api/registry/instances` | 查看在线工具实例 |

### 3. 接入工具服务（Example）

```bash
java -jar nanobot4j-example/target/nanobot4j-example-1.0.0-SNAPSHOT.jar
```

Example 启动后自动向 Admin 注册，Admin 即可调用其工具。

### 4. 发起流式对话

```bash
# 新会话（自动生成 sessionId）
curl -X POST http://localhost:8080/api/agent/stream/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我计算 123 + 456"}' \
  --no-buffer

# 多轮对话（传入 sessionId 保持记忆）
curl -X POST http://localhost:8080/api/agent/stream/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "my-session-001", "message": "刚才的结果再乘以 2"}' \
  --no-buffer
```

SSE 响应示例：

```
data: {"type":"THINKING","content":"📚 已加载 3 条历史记忆"}
data: {"type":"THINKING","content":"🧠 用户需要计算加法，使用 calculator 工具"}
data: {"type":"TOOL_CALL","toolName":"calculator","toolArgs":"{\"a\":123,\"b\":456}"}
data: {"type":"TOOL_RESULT","toolName":"calculator","toolResult":"579"}
data: {"type":"FINAL_ANSWER","content":"123 + 456 = 579"}
data: {"type":"DONE"}
```

---

## Roadmap

| 状态 | 特性 |
|------|------|
| ✅ 已完成 | 多级记忆（Redis + 滑动窗口 + 异步摘要） |
| ✅ 已完成 | 动态工具自举（Groovy 沙箱 + ToolCreatorTool） |
| ✅ 已完成 | 工业级 ReAct 引擎（熔断器 + 防死循环 + thinking 标签） |
| ✅ 已完成 | SSE 流式推流 + 虚拟线程并发 |
| ✅ 已完成 | 去中心化工具治理（心跳注册 + 动态 RPC） |
| 🔲 规划中 | **凭证保险箱（Credential Vault）注入**：支持动态 Groovy 脚本安全调用需鉴权的外部 API，凭证加密存储，运行时注入，脚本代码中不暴露明文密钥 |
| 🔲 规划中 | **多 LLM 路由**：根据任务复杂度自动路由到不同模型（GPT-4o / Claude / 本地 Ollama），降低推理成本 |
| 🔲 规划中 | **工具调用链追踪**：集成 OpenTelemetry，对每次 ReAct 循环生成完整 Trace，支持 Jaeger / Zipkin 可视化 |
| 🔲 规划中 | **Agent 协作（Multi-Agent）**：支持 Admin 将子任务委派给专属 Sub-Agent，实现并行工具调用 |
| 🔲 规划中 | **工具版本管理**：动态工具支持版本控制与回滚，防止 LLM 生成的脚本覆盖稳定版本 |
| 🔲 规划中 | **Web 管理界面**：可视化展示在线工具实例、会话记忆、ReAct 执行轨迹 |

---

## License

MIT License © 2024 Nanobot4J Contributors
