# Nanobot4J 部署指南

本文档详细说明如何部署和运维 Nanobot4J SSE 流式服务。

## 📋 目录

- [环境要求](#环境要求)
- [快速部署](#快速部署)
- [启动脚本详解](#启动脚本详解)
- [停止脚本详解](#停止脚本详解)
- [日志管理](#日志管理)
- [监控和统计](#监控和统计)
- [常见问题](#常见问题)
- [生产环境配置](#生产环境配置)

## 环境要求

### 必需环境

- **Java**: 17 或更高版本
- **Maven**: 3.6+ 
- **操作系统**: Linux / macOS / Windows (WSL)
- **内存**: 最低 2GB，推荐 4GB+
- **磁盘**: 最低 1GB 可用空间

### API Key

需要至少一个 LLM 提供商的 API Key：

- **DeepSeek API Key** (推荐)
- **Kimi/Moonshot API Key** (可选)

## 快速部署

### 1. 克隆项目

```bash
git clone https://github.com/your-org/nanobot4J.git
cd nanobot4J
```

### 2. 配置环境变量

创建 `.env` 文件：

```bash
cat > .env << 'ENVEOF'
DEEPSEEK_API_KEY=sk-your-deepseek-api-key
KIMI_API_KEY=sk-your-kimi-api-key
ENVEOF
```

### 3. 启动服务

```bash
# 赋予执行权限
chmod +x start-generic.sh stop.sh

# 启动所有服务
./start-generic.sh
```

### 4. 验证部署

访问以下地址验证服务是否正常：

- **健康检查**: http://localhost:8080/actuator/health
- **SSE流式对话**: http://localhost:8080/chat-stream.html
- **管理控制台**: http://localhost:8080

## 启动脚本详解

### start-generic.sh

#### 脚本功能

1. **环境检查**
   - 检查 `.env` 文件是否存在
   - 验证 Java 和 Maven 环境
   - 加载环境变量

2. **项目构建**
   - 执行 `mvn clean install -DskipTests`
   - 构建所有模块

3. **服务启动**
   - 启动 Admin 控制台（端口 8080）
   - 启动 Client 应用（端口 8081）
   - 后台运行并记录 PID

4. **状态验证**
   - 检查服务是否启动成功
   - 验证工具注册情况
   - 显示访问地址

#### 执行流程

```
开始
  ↓
检查 .env 文件
  ↓
加载环境变量
  ↓
构建项目
  ↓
启动 Admin (8080)
  ↓
等待 15 秒
  ↓
启动 Client (8081)
  ↓
等待 15 秒
  ↓
验证服务状态
  ↓
显示访问信息
  ↓
完成
```

## 停止脚本详解

### stop.sh

#### 脚本功能

1. **进程查找**
   - 查找所有 AdminApplication 进程
   - 查找所有 ExampleApplication 进程

2. **优雅关闭**
   - 发送 SIGTERM 信号
   - 等待进程正常退出

3. **强制终止**
   - 检查残留进程
   - 必要时发送 SIGKILL 信号

4. **清理工作**
   - 保留日志文件
   - 显示日志位置

## 日志管理

### 日志位置

| 服务 | 日志文件 | 说明 |
|------|---------|------|
| Admin | `/tmp/admin.log` | Admin 控制台日志 |
| Client | `/tmp/client.log` | Client 应用日志 |

### 查看日志

#### 实时查看

```bash
# 实时查看 Admin 日志
tail -f /tmp/admin.log

# 实时查看 Client 日志
tail -f /tmp/client.log

# 同时查看两个日志
tail -f /tmp/admin.log /tmp/client.log
```

#### 搜索日志

```bash
# 搜索错误日志
grep ERROR /tmp/admin.log

# 搜索警告日志
grep WARN /tmp/admin.log

# 搜索 SSE 相关日志
grep SSE /tmp/admin.log

# 搜索工具调用日志
grep "Tool execution" /tmp/admin.log
```

#### 查看最近日志

```bash
# 查看最近 100 行
tail -n 100 /tmp/admin.log

# 查看最近 50 行并实时更新
tail -n 50 -f /tmp/admin.log
```

## 监控和统计

### 健康检查

```bash
# 检查服务健康状态
curl http://localhost:8080/actuator/health
```

### SSE 连接统计

```bash
# 查看活跃 SSE 连接数
curl http://localhost:8080/api/agent/stream/stats
```

### 工具注册情况

```bash
# 查看所有注册的工具实例
curl http://localhost:8080/api/registry/instances
```

## 常见问题

### 1. 端口被占用

**问题**: 启动失败，提示端口 8080 或 8081 已被占用

**解决方案**:

```bash
# 查找占用端口的进程
lsof -i :8080
lsof -i :8081

# 终止进程
kill -9 <PID>
```

### 2. 环境变量未生效

**问题**: LLM API 调用失败，提示 API Key 无效

**解决方案**:

```bash
# 检查 .env 文件格式
cat .env

# 确保格式正确（无空格，无引号）
DEEPSEEK_API_KEY=sk-xxxxx
KIMI_API_KEY=sk-xxxxx

# 重新启动服务
./stop.sh
./start-generic.sh
```

### 3. 服务启动失败

**问题**: 启动脚本执行后服务未正常运行

**解决方案**:

```bash
# 查看详细日志
tail -f /tmp/admin.log

# 检查 Java 版本（需要 Java 17+）
java -version

# 手动构建项目
mvn clean install -DskipTests
```

### 4. SSE 连接断开

**问题**: 流式对话中途断开连接

**原因**:
- SSE 连接默认超时 5 分钟
- 网络不稳定
- 服务器资源不足

**解决方案**:

```bash
# 检查服务器日志
grep "SSE" /tmp/admin.log

# 检查系统资源
top
free -h
```

## 生产环境配置

### Nginx 反向代理

配置 Nginx 支持 SSE：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }

    # SSE 流式接口特殊配置
    location /api/agent/stream/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;  # 关键：禁用缓冲
        proxy_cache off;
        proxy_read_timeout 600s;  # 10 分钟超时
    }
}
```

## 总结

本文档涵盖了 Nanobot4J SSE 流式服务的完整部署流程。

如有问题，请查看：
- [SSE 实现文档](../SSE_IMPLEMENTATION.md)
- [项目 README](../README.md)
