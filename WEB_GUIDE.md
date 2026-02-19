# Web 交互层使用指南

## 🎉 已完成的功能

### 1. Swagger API 文档
- **访问地址**: http://localhost:8080/swagger-ui.html
- **功能**: 提供完整的 API 文档和在线测试

### 2. REST API 接口

#### POST /api/chat
发送聊天消息

**请求示例**:
```json
{
  "message": "你好，请问今天天气怎么样？",
  "sessionId": "user123"
}
```

**响应示例**:
```json
{
  "reply": "你好！我是 Nanobot4J，很高兴为你服务。",
  "status": "success",
  "sessionId": "user123",
  "error": null
}
```

#### GET /api/health
健康检查接口

**响应**: `OK`

### 3. 极简聊天页面
- **访问地址**: http://localhost:8080/
- **功能**:
  - 实时聊天界面
  - 自动保存会话 ID
  - 加载动画
  - 错误提示
  - ChatGPT 风格的 UI

## 🚀 快速开始

### 1. 启动应用
```bash
mvn spring-boot:run
```

### 2. 访问聊天页面
打开浏览器访问: http://localhost:8080/

### 3. 开始聊天
- 在输入框中输入消息
- 点击"发送"按钮或按 Enter 键
- 等待 AI 回复

### 4. 查看 API 文档
访问: http://localhost:8080/swagger-ui.html

## 📋 技术栈

- **后端框架**: Spring Boot 3.2.2
- **API 文档**: SpringDoc OpenAPI (Swagger)
- **前端**: 原生 HTML + CSS + JavaScript
- **HTTP 客户端**: Fetch API

## 🎨 UI 特性

- **渐变背景**: 紫色渐变背景
- **消息气泡**: 用户消息（紫色）和 AI 消息（白色）
- **加载动画**: 三个跳动的圆点
- **响应式设计**: 适配不同屏幕尺寸
- **平滑动画**: 消息淡入效果

## 🔧 配置说明

### 会话 ID
- 自动生成并保存在 localStorage
- 格式: `user_<timestamp>`
- 可在浏览器控制台查看: `localStorage.getItem('nanobot_session_id')`

### API 端点
默认使用相对路径 `/api/chat`，如需修改请编辑 `index.html` 中的 fetch URL。

## 📝 API 使用示例

### 使用 curl 测试
```bash
# 发送消息
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好",
    "sessionId": "user123"
  }'

# 健康检查
curl http://localhost:8080/api/health
```

### 使用 JavaScript 测试
```javascript
fetch('http://localhost:8080/api/chat', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    message: '你好',
    sessionId: 'user123'
  })
})
.then(response => response.json())
.then(data => console.log(data));
```

## 🐛 故障排查

### 问题 1: 无法连接到服务器
- 确认应用已启动
- 检查端口 8080 是否被占用
- 查看控制台日志

### 问题 2: Session not found
- 确保数据库已初始化
- 检查 `user123` 会话是否存在
- 应用会在启动时自动创建默认会话

### 问题 3: LLM 调用失败
- 检查 LLM 配置（Ollama 或 OpenAI）
- 确认 API Key 已配置
- 查看应用日志获取详细错误信息

## 📚 相关文档

- [MULTI_MODEL_GUIDE.md](MULTI_MODEL_GUIDE.md) - 多模型配置指南
- [QUICK_START.md](QUICK_START.md) - 快速开始指南
- [ARCHITECTURE.md](ARCHITECTURE.md) - 架构文档

## 🎯 下一步

1. **自定义 UI**: 修改 `index.html` 中的样式
2. **添加功能**: 扩展 ChatController 添加更多接口
3. **集成认证**: 添加用户认证和授权
4. **部署上线**: 配置生产环境并部署

---

享受使用 Nanobot4J！🚀
