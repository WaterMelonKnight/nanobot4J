# 🚀 Nanobot4J 泛型 ReAct Agent - 快速启动指南

## 📋 概述

本项目实现了一个**完全泛型化、零硬编码**的 ReAct Agent 系统，支持：
- ✅ 真实 LLM 调用（DeepSeek/Kimi）
- ✅ 动态工具发现
- ✅ 完整的 ReAct 循环（思考→行动→观察→回答）
- ✅ 美观的 Web 界面

---

## 🔧 前置准备

### 1. 配置 API Key

在项目根目录创建 `.env` 文件：

```bash
# DeepSeek API Key
DEEPSEEK_API_KEY="your-deepseek-api-key"

# Kimi (Moonshot) API Key
KIMI_API_KEY="your-kimi-api-key"
```

### 2. 系统要求

- Java 17+
- Maven 3.6+
- 端口 8080 和 8081 可用

---

## 🚀 快速启动

### 方式一：使用启动脚本（推荐）

```bash
# 启动所有服务
./start-generic.sh

# 停止所有服务
./stop.sh
```

### 方式二：手动启动

```bash
# 1. 加载环境变量
export $(cat .env | grep -v '^#' | xargs)

# 2. 构建项目
mvn clean install -DskipTests -f pom-parent.xml

# 3. 启动 Admin 服务
cd nanobot4j-admin
mvn spring-boot:run &

# 4. 启动 Client 服务
cd ../nanobot4j-example
mvn spring-boot:run &
```

---

## 🌐 访问服务

| 服务 | 地址 | 说明 |
|------|------|------|
| **泛型 Agent 对话页面** | http://localhost:8080/chat-generic.html | 🎨 推荐 |
| Admin Dashboard | http://localhost:8080 | 管理控制台 |
| Client 应用 | http://localhost:8081 | 工具提供方 |

---

## 💬 使用示例

打开 http://localhost:8080/chat-generic.html，尝试：

1. **天气查询**: "上海的天气怎么样？"
2. **时间查询**: "现在几点了？"
3. **数学计算**: "帮我计算 50 乘以 3"

---

## 🔍 查看日志

```bash
# Admin 服务日志
tail -f /tmp/admin.log

# Client 服务日志
tail -f /tmp/client.log
```

---

## 🛑 停止服务

```bash
./stop.sh
```

---

## 📚 更多文档

- [泛型重构总结](./GENERIC_REFACTORING_SUMMARY.md)
- [测试结果报告](./GENERIC_AGENT_TEST_RESULTS.md)
