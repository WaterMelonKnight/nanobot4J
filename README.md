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

### 4. 轻量级设计
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

## License

MIT
