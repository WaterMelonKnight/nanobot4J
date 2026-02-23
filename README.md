# Nanobot4J - Java Agent Framework

一个轻量级的 Java Agent 框架，支持工具注册、服务发现和集中管理。

## 项目结构

```
nanobot4J/
├── nanobot4j-core/                 # 核心 SDK（无 Spring 依赖）
│   ├── tool/                       # 工具接口定义
│   ├── memory/                     # 记忆管理
│   ├── agent/                      # Agent 核心
│   └── llm/                        # LLM 客户端接口
├── nanobot4j-spring-boot-starter/  # Spring Boot 自动装配
│   ├── annotation/                 # @NanobotTool 注解
│   ├── registry/                   # 工具注册表
│   └── autoconfigure/              # 自动配置
├── nanobot4j-admin/                # 管理控制台
│   ├── controller/                 # REST API
│   ├── service/                    # 实例注册表
│   └── resources/static/           # Dashboard 页面
└── nanobot4j-example/              # 示例应用
    └── tools/                      # 示例工具
```

## 快速开始

### 1. 构建项目

```bash
cd /workspace/nanobot4J
mvn clean install -DskipTests
```

### 2. 启动 Admin 控制台

```bash
cd nanobot4j-admin
mvn spring-boot:run
```

访问: http://localhost:8080

### 3. 启动示例应用

```bash
cd nanobot4j-example
mvn spring-boot:run
```

示例应用会自动注册到 Admin 控制台，并每 30 秒发送心跳。

### 4. 查看 Dashboard

打开浏览器访问 http://localhost:8080，你将看到：
- 左侧：已注册的服务实例列表
- 右侧：选中实例的工具详情

### 5. 体验 AI Agent 对话

#### 方式一：使用启动脚本（推荐）

```bash
# 配置 API Key（在项目根目录创建 .env 文件）
echo 'DEEPSEEK_API_KEY=your-deepseek-api-key' > .env
echo 'KIMI_API_KEY=your-kimi-api-key' >> .env

# 一键启动所有服务
./start-generic.sh
```

#### 方式二：流式对话体验

访问流式对话页面：http://localhost:8080/chat-stream.html

**特性**：
- ✅ 实时推送思考过程（SSE）
- ✅ 逐步展示工具调用和结果
- ✅ 基于异步线程池的高并发支持
- ✅ 完整的 ReAct 流程可视化

#### 方式三：传统对话

访问泛型对话页面：http://localhost:8080/chat-generic.html

尝试以下示例：
- **数学计算**: "帮我计算 25 加 25"
- **天气查询**: "上海的天气怎么样？"
- **时间查询**: "现在几点了？"

## 使用方式

### 在你的项目中使用

1. 添加依赖：

```xml
<dependency>
    <groupId>com.nanobot</groupId>
    <artifactId>nanobot4j-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. 配置 application.yml：

```yaml
nanobot:
  admin:
    enabled: true
    url: http://localhost:8080
    heartbeat-interval: 30
```

3. 创建工具：

```java
@Component
public class MyTools {

    @NanobotTool(
        name = "my_tool",
        description = "我的工具描述",
        parameterSchema = """
            {
              "type": "object",
              "properties": {
                "param1": {"type": "string"}
              }
            }
            """
    )
    public String myTool(Map<String, Object> params) {
        // 实现你的工具逻辑
        return "result";
    }
}
```

## 核心特性

### 1. 工具自动注册
使用 `@NanobotTool` 注解标记方法，框架会自动扫描并注册。

### 2. 服务发现
应用启动时自动注册到 Admin 控制台，支持心跳检测。

### 3. 集中管理
通过 Admin Dashboard 查看所有在线服务及其提供的工具。

### 4. AI Agent 对话系统

#### 🚀 泛型 ReAct Agent（零硬编码）

基于真实 LLM（DeepSeek/Kimi）的完全动态化 Agent 系统：

**核心特性**：
- ✅ **零硬编码**：无任何工具名称硬编码，完全动态发现
- ✅ **真实 LLM**：集成 DeepSeek 和 Kimi API
- ✅ **完整 ReAct 循环**：思考 → 行动 → 观察 → 回答
- ✅ **动态工具发现**：自动从注册中心获取所有在线工具
- ✅ **JSON 协议**：标准化的工具调用格式
- ✅ **参数类型保留**：正确处理数字、字符串、布尔值等类型

**访问地址**：
- 泛型对话页面：http://localhost:8080/chat-generic.html
- 流式对话页面：http://localhost:8080/chat-stream.html

#### 🌊 SSE 流式输出（v1.3 新增）

基于 Server-Sent Events 的实时流式推送系统：

**技术架构**：
- **后端**：Spring Boot SSE + 异步线程池
- **前端**：Fetch API ReadableStream
- **事件协议**：结构化的 AgentStreamEvent

**事件类型**：
```java
- THINKING      // 思考过程
- TOOL_CALL     // 工具调用（含工具名和参数）
- TOOL_RESULT   // 工具执行结果
- FINAL_ANSWER  // 最终答案
- DONE          // 任务完成
- ERROR         // 异常信息
```

**实时推送示例**：
```
1. THINKING    → "🤔 开始分析任务..."
2. THINKING    → "💭 TOOL_CALL: {\"name\": \"calculator\", ...}"
3. TOOL_CALL   → toolName: "calculator", args: {...}
4. TOOL_RESULT → "25.00 add 25.00 = 50.00"
5. THINKING    → "💭 FINAL_ANSWER: 计算结果是 50"
6. FINAL_ANSWER → "计算结果是 50"
7. DONE        → 任务完成
```

**性能优势**：
- 使用缓存线程池处理并发连接
- 支持数千个同时在线的 SSE 连接
- 实时推送，无需轮询
- 自动超时管理（5 分钟）

#### 📊 监控接口

- **活跃连接数**：http://localhost:8080/api/agent/stream/stats
- **注册实例**：http://localhost:8080/api/registry/instances

### 5. 轻量级设计
- Core 模块无 Spring 依赖
- Starter 模块提供开箱即用的自动配置
- Admin 控制台使用简单的 HTML + Bootstrap

## 架构设计

### 模块职责

- **nanobot4j-core**: 提供核心抽象（Tool, Memory, Agent, LLM）
- **nanobot4j-spring-boot-starter**: Spring Boot 集成，自动扫描和注册
- **nanobot4j-admin**: 服务注册中心和管理控制台

### 注册机制

1. 应用启动时，`AdminReporter` 监听 `ApplicationReadyEvent`
2. 收集本地注册的所有工具信息
3. 向 Admin 发送注册请求（POST /api/registry/register）
4. 启动定时任务，每 30 秒发送心跳（POST /api/registry/beat）
5. Admin 每 30 秒检查实例状态，超过 90 秒无心跳标记为 OFFLINE

## 技术栈

- Java 17+
- Spring Boot 3.2.2
- Maven
- Lombok
- OkHttp
- Jackson
- Bootstrap 5
- **DeepSeek API** (LLM)
- **Kimi/Moonshot API** (LLM)
- **Server-Sent Events** (SSE)
- **异步线程池** (高并发)

## 功能演示

### 🌊 SSE 流式 ReAct Agent（推荐）

#### 实时推送演示

访问 http://localhost:8080/chat-stream.html，输入 "帮我计算 25 加 25"

**实时推送过程**：
```
[1] THINKING    → 🤔 开始分析任务...
[2] THINKING    → 💭 TOOL_CALL: {"name": "calculator", "args": {...}}
[3] TOOL_CALL   → 🔧 调用工具: calculator
                  参数: {"operation": "add", "a": 25, "b": 25}
[4] TOOL_RESULT → 📊 工具返回: 25.00 add 25.00 = 50.00
[5] THINKING    → 💭 FINAL_ANSWER: 25 加 25 的计算结果是 50
[6] FINAL_ANSWER → ✨ 最终答案: 25 加 25 的计算结果是 50
[7] DONE        → ✅ 任务完成
```

**技术实现**：
- 每个步骤实时推送到前端
- 无需等待整个流程完成
- 用户可见完整的思考过程
- 支持中断和错误处理

#### 架构设计

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│   Browser   │  SSE    │  StreamAgent     │   RPC   │   Client    │
│  (Frontend) │◄────────│   Controller     │◄────────│  (Tools)    │
└─────────────┘         └──────────────────┘         └─────────────┘
                                 │
                                 ▼
                        ┌──────────────────┐
                        │  Thread Pool     │
                        │  (Async Exec)    │
                        └──────────────────┘
                                 │
                                 ▼
                        ┌──────────────────┐
                        │ Streaming        │
                        │ ReAct Engine     │
                        └──────────────────┘
                                 │
                                 ▼
                        ┌──────────────────┐
                        │  LLM Service     │
                        │  (DeepSeek/Kimi) │
                        └──────────────────┘
```

### 🤖 泛型 ReAct Agent

#### 零硬编码设计

**传统方式**（硬编码）：
```java
// ❌ 不推荐：硬编码工具名称
if (message.contains("天气")) {
    return weatherTool.execute(params);
}
```

**泛型方式**（动态发现）：
```java
// ✅ 推荐：完全动态化
List<ToolMetadata> tools = getAvailableTools();  // 从注册中心获取
String prompt = buildDynamicPrompt(tools);        // 动态构建 Prompt
String llmResponse = llmService.chat(prompt);     // LLM 决策
ToolCall toolCall = parseLLMResponse(llmResponse); // 解析调用
String result = executeRemoteTool(toolCall);      // 远程执行
```

**优势**：
- 新增工具无需修改 Admin 代码
- 支持任意数量的工具
- 工具可以动态上下线
- 完全符合开闭原则（OCP）

### 访问地址

- **服务管理 Dashboard**: http://localhost:8080/
- **泛型 Agent 对话**: http://localhost:8080/chat-generic.html
- **流式 Agent 对话**: http://localhost:8080/chat-stream.html
- **API 端点（同步）**: http://localhost:8080/api/agent/generic/chat
- **API 端点（流式）**: http://localhost:8080/api/agent/stream/chat
- **监控统计**: http://localhost:8080/api/agent/stream/stats
- **注册实例**: http://localhost:8080/api/registry/instances

## 开发进度

### ✅ 已完成

#### Phase 1: 基础架构 (v1.0)
- [x] 多模块 Maven 项目结构
- [x] 核心 SDK 设计（nanobot4j-core）
- [x] Spring Boot Starter 自动装配
- [x] @NanobotTool 注解支持
- [x] 工具自动扫描和注册

#### Phase 2: 服务治理 (v1.1)
- [x] Admin 管理控制台
- [x] 服务注册与发现机制
- [x] 心跳检测和健康检查
- [x] 实例状态管理（ONLINE/OFFLINE）
- [x] Dashboard 可视化界面

#### Phase 3: 泛型 ReAct Agent (v1.2)
- [x] 完全泛型化的 Agent 架构（零硬编码）
- [x] 真实 LLM 集成（DeepSeek/Kimi）
- [x] 动态工具发现和注入
- [x] 完整 ReAct 循环实现
- [x] JSON 工具调用协议
- [x] 参数类型智能处理
- [x] 远程工具 RPC 调用
- [x] 泛型对话 UI（chat-generic.html）

#### Phase 4: SSE 流式输出 (v1.3) 🆕
- [x] AgentStreamEvent 事件协议设计
- [x] StreamAgentController SSE 控制器
- [x] StreamingGenericReActAgent 流式执行引擎
- [x] 异步线程池架构
- [x] 实时事件推送（6 种事件类型）
- [x] 前端 ReadableStream 处理
- [x] 流式对话 UI（chat-stream.html）
- [x] 连接监控和统计接口

### 🚧 进行中

- [ ] 多轮对话上下文管理
- [ ] 会话历史持久化
- [ ] 更多 LLM 提供商支持（OpenAI/Claude）

### 📋 计划中

- [ ] 工具市场和插件系统
- [ ] Agent 编排和工作流
- [ ] 分布式工具调用优化
- [ ] 性能监控和链路追踪
- [ ] 安全认证和权限管理
- [ ] WebSocket 双向通信
- [ ] 多 Agent 协作

## License

MIT
