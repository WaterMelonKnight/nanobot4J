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

访问增强版对话页面：http://localhost:8080/chat-enhanced.html

尝试以下示例：
- **多工具协同**: "将上海的气温与杭州的气温相加"
- **数学计算**: "计算 100 * 25"
- **天气查询**: "深圳今天天气怎么样？"

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
- **多步骤工具链式调用**: 支持复杂任务的自动分解和执行
- **思考过程可视化**: 完整展示 Agent 的推理链路和决策过程
- **智能任务规划**: 自动分析用户意图，生成最优执行计划
- **上下文管理**: 步骤间结果自动传递，支持工具协同

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

- Java 17
- Spring Boot 3.2.2
- Maven
- Lombok
- OkHttp
- Jackson
- Bootstrap 5

## 功能演示

### AI Agent 对话系统

增强版 AI Agent 支持复杂的多步骤任务处理，具备以下能力：

#### 1. 多工具链式调用

**示例**: "将上海的气温与杭州的气温相加"

```
思考过程：
🤔 分析任务：将上海的气温与杭州的气温相加
📋 执行计划：
  步骤 1: 查询上海的天气
  步骤 2: 查询杭州的天气
  步骤 3: 计算气温总和

执行结果：
⚙️ 步骤 1: 查询上海的天气 → 25°C
⚙️ 步骤 2: 查询杭州的天气 → 23°C
⚙️ 步骤 3: 计算气温总和 → 48.0°C

💡 最终答案: 48.0°C
```

#### 2. 单工具调用

**计算示例**: "计算 100 * 25"
- 自动识别数学运算
- 提取操作数和运算符
- 返回结果: 2500.0

**查询示例**: "深圳今天天气怎么样？"
- 识别城市名称
- 调用天气工具
- 返回结果: 深圳气温：28°C

#### 3. 思考过程可视化

对话界面完整展示：
- ✅ 任务分析过程
- ✅ 执行计划步骤
- ✅ 工具调用详情（工具名称 + 参数）
- ✅ 每步执行结果
- ✅ 最终答案汇总

### 访问地址

- **服务管理**: http://localhost:8080/
- **AI 对话（增强版）**: http://localhost:8080/chat-enhanced.html
- **API 端点**: http://localhost:8080/api/agent/chat

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

#### Phase 3: AI Agent 系统 (v1.2)
- [x] 基础 Agent 对话功能
- [x] 单工具智能调用
- [x] 多步骤任务规划引擎
- [x] 工具链式调用支持
- [x] 上下文管理和结果传递
- [x] 思考过程追踪和展示
- [x] 增强版对话 UI

### 🚧 进行中

- [ ] LLM 集成（支持 OpenAI/Claude API）
- [ ] 记忆系统实现
- [ ] 更多内置工具

### 📋 计划中

- [ ] 工具市场
- [ ] Agent 编排能力
- [ ] 分布式工具调用
- [ ] 性能监控和日志
- [ ] 安全认证机制

## License

MIT
