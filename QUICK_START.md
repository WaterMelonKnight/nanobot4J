# Nanobot4J 快速启动指南

## 🚀 项目已成功重构为多模块架构

### 项目结构

```
nanobot4J/
├── nanobot4j-core/                 # 核心 SDK（无 Spring 依赖）
├── nanobot4j-spring-boot-starter/  # Spring Boot 自动装配
├── nanobot4j-admin/                # 管理控制台
└── nanobot4j-example/              # 示例应用
```

## 📦 构建项目

```bash
cd /workspace/nanobot4J
mvn -f pom-parent.xml clean install -DskipTests
```

## 🎯 启动服务

### 1. 启动 Admin 控制台（端口 8080）

```bash
cd nanobot4j-admin
mvn spring-boot:run
```

### 2. 启动示例应用（端口 8081）

```bash
cd nanobot4j-example
mvn spring-boot:run
```

## 🌐 访问 Dashboard

打开浏览器访问：**http://localhost:8080**

你将看到：
- 左侧：已注册的服务实例列表
- 右侧：选中实例的工具详情

## 📊 API 接口

### 查询所有实例
```bash
curl http://localhost:8080/api/registry/instances
```

### 查询在线实例
```bash
curl http://localhost:8080/api/registry/instances/online
```

## 🔧 在你的项目中使用

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.nanobot</groupId>
    <artifactId>nanobot4j-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置 application.yml

```yaml
server:
  port: 8081

spring:
  application:
    name: my-service

nanobot:
  admin:
    enabled: true
    address: http://localhost:8080
    heartbeat-interval: 30
```

### 3. 创建工具

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
                "param1": {"type": "string", "description": "参数1"}
              },
              "required": ["param1"]
            }
            """
    )
    public String myTool(Map<String, Object> params) {
        String param1 = (String) params.get("param1");
        return "处理结果: " + param1;
    }
}
```

## ✨ 核心特性

1. **自动工具注册** - 使用 @NanobotTool 注解，框架自动扫描并注册
2. **服务自动发现** - 应用启动时自动注册到 Admin
3. **心跳检测** - 每 30 秒发送心跳，90 秒无响应标记为 OFFLINE
4. **可视化管理** - Dashboard 实时显示所有服务和工具
5. **轻量级设计** - Core 模块无 Spring 依赖，可独立使用

## 🎓 示例工具

示例应用已注册 3 个工具：

1. **calculator** - 数学计算（加减乘除）
2. **weather** - 天气查询（模拟数据）
3. **time** - 获取当前系统时间

## 🐛 故障排查

### 服务未注册成功？

1. 检查 Admin 是否启动：`curl http://localhost:8080/api/registry/instances`
2. 检查配置文件中的 `nanobot.admin.address` 是否正确
3. 查看应用日志，确认是否有 "Successfully registered to Admin" 消息

### 工具未被发现？

1. 确保类上有 `@Component` 注解
2. 确保方法上有 `@NanobotTool` 注解
3. 检查日志中是否有 "Discovered @NanobotTool" 消息

## 📝 注意事项

- Admin 必须先启动，客户端才能注册成功
- 配置文件中使用 `address` 而不是 `url`
- Spring Boot 3.x 需要 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件

## 🎉 成功标志

启动成功后，日志中应该看到：

```
INFO c.n.s.a.NanobotAutoConfiguration : Initializing ToolRegistry
INFO c.n.s.a.NanobotAutoConfiguration : Initializing ToolScanner
INFO c.nanobot.starter.registry.ToolRegistry : Registered tool: xxx
INFO c.n.starter.registry.AdminReporter : Successfully registered to Admin
INFO c.n.starter.registry.AdminReporter : Heartbeat started with interval: 30 seconds
```

---

**祝你使用愉快！** 🚀
