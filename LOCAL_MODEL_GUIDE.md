# Nanobot4J 本地模型使用指南

## 概述

Nanobot4J 现在支持本地量化模型（如 BitNet b1.58、DeepSeek-R1-Distill），大幅降低推理成本。

**核心特性：**
- ✅ 支持 Ollama 本地模型
- ✅ 自动降级策略（本地失败时切换到云端）
- ✅ 针对小模型优化的 Prompt 模板（ChatML、Alpaca）
- ✅ 超时控制和错误处理

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                    AgentService                          │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              FallbackLLMClient                           │
│         (模型降级策略)                                    │
└────────┬────────────────────────┬───────────────────────┘
         │                        │
    优先使用                   失败时降级
         │                        │
         ▼                        ▼
┌──────────────────┐    ┌──────────────────┐
│ OllamaLLMClient  │    │ SpringAILLMClient│
│  (本地模型)       │    │  (云端模型)       │
└────────┬─────────┘    └────────┬─────────┘
         │                        │
         ▼                        ▼
┌──────────────────┐    ┌──────────────────┐
│ ChatMLTemplate   │    │  OpenAI API      │
│ (Prompt 格式化)  │    │  DeepSeek API    │
└──────────────────┘    └──────────────────┘
```

## 快速开始

### 1. 安装 Ollama

```bash
# macOS/Linux
curl -fsSL https://ollama.com/install.sh | sh

# 或者使用 Docker
docker run -d -p 11434:11434 --name ollama ollama/ollama
```

### 2. 下载模型

```bash
# 下载 BitNet 模型（示例）
ollama pull bitnet

# 或者其他量化模型
ollama pull deepseek-r1-distill-qwen-1.5b
ollama pull llama3.2:1b
ollama pull qwen2.5:0.5b
```

### 3. 配置 Nanobot4J

编辑 `application.properties`：

```properties
# 使用本地模型作为主要推理后端
nanobot.llm.primary=ollama

# Ollama 配置
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=bitnet
spring.ai.ollama.chat.options.temperature=0.7

# 云端模型作为备用（可选）
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4

# 降级策略
nanobot.llm.fallback.timeout-ms=30000
nanobot.llm.fallback.enabled=true

# Prompt 模板（chatML 或 alpaca）
nanobot.llm.prompt-template=chatML
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

## 模型降级策略

### 工作原理

`FallbackLLMClient` 实现了智能降级：

1. **优先使用本地模型**
   - 调用 Ollama 本地模型
   - 设置 30 秒超时

2. **自动降级到云端**
   - 如果本地模型超时
   - 如果本地模型返回错误
   - 如果响应格式不正确

3. **记录降级事件**
   - 统计成功/失败次数
   - 计算降级率
   - 用于监控和优化

### 降级触发条件

```java
// 1. 超时
if (duration > 30000ms) {
    fallback to cloud
}

// 2. 错误
catch (Exception e) {
    fallback to cloud
}

// 3. 格式错误（未来扩展）
if (!isValidJSON(response)) {
    fallback to cloud
}
```

### 查看降级统计

```java
@Autowired
private FallbackLLMClient fallbackClient;

public void checkStats() {
    var stats = fallbackClient.getStats();
    System.out.println("Primary success: " + stats.primarySuccessCount());
    System.out.println("Fallback count: " + stats.fallbackCount());
    System.out.println("Fallback rate: " + stats.fallbackRate());
}
```

## Prompt 模板

### ChatML 格式

适用于大多数现代小模型（BitNet、Qwen、LLaMA 3.2）。

**格式示例：**
```
<|im_start|>system
You are a helpful assistant.<|im_end|>
<|im_start|>user
Hello!<|im_end|>
<|im_start|>assistant
Hi! How can I help you?<|im_end|>
<|im_start|>assistant
```

**使用：**
```properties
nanobot.llm.prompt-template=chatML
```

### Alpaca 格式

适用于基于 LLaMA 的模型。

**格式示例：**
```
Below is an instruction that describes a task. Write a response that appropriately completes the request.

### Instruction:
You are a helpful assistant.

### Input:
Hello!

### Response:
Hi! How can I help you?

### Response:
```

**使用：**
```properties
nanobot.llm.prompt-template=alpaca
```

## 支持的模型

### 推荐的本地模型

| 模型 | 大小 | 速度 | 质量 | 适用场景 |
|------|------|------|------|----------|
| BitNet b1.58 | ~1GB | 极快 | 中等 | 简单对话、工具调用 |
| DeepSeek-R1-Distill-Qwen-1.5B | ~1.5GB | 快 | 好 | 推理任务 |
| LLaMA 3.2 1B | ~1GB | 快 | 好 | 通用对话 |
| Qwen2.5 0.5B | ~500MB | 极快 | 中等 | 轻量级任务 |
| Phi-3 Mini | ~2GB | 中等 | 很好 | 复杂推理 |

### 下载命令

```bash
# BitNet
ollama pull bitnet

# DeepSeek
ollama pull deepseek-r1-distill-qwen-1.5b

# LLaMA 3.2
ollama pull llama3.2:1b

# Qwen
ollama pull qwen2.5:0.5b

# Phi-3
ollama pull phi3:mini
```

## 使用示例

### 示例 1：纯本地推理

```bash
# 1. 启动 Ollama
ollama serve

# 2. 配置使用本地模型
nanobot.llm.primary=ollama
nanobot.llm.fallback.enabled=false

# 3. 创建会话
curl -X POST http://localhost:8080/api/v1/sessions \
  -H "Content-Type: application/json" \
  -d '{"agentId": "math-assistant", "userId": "user123"}'

# 4. 发送消息
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "xxx", "message": "What is 50 + 50?"}'
```

### 示例 2：本地优先 + 云端备用

```bash
# 配置
nanobot.llm.primary=ollama
nanobot.llm.fallback.enabled=true

# 正常情况：使用本地模型
# 本地失败时：自动切换到 OpenAI/DeepSeek
```

### 示例 3：使用 DeepSeek API 作为备用

```properties
# 使用 DeepSeek 作为云端备用（比 OpenAI 便宜）
spring.ai.openai.base-url=https://api.deepseek.com/v1
spring.ai.openai.api-key=${DEEPSEEK_API_KEY}
spring.ai.openai.chat.options.model=deepseek-chat
```

## 性能优化

### 1. 选择合适的模型

- **简单任务**：使用 0.5B-1B 模型（Qwen2.5:0.5b）
- **中等任务**：使用 1B-3B 模型（LLaMA 3.2:1b）
- **复杂任务**：使用 3B+ 模型或云端模型

### 2. 调整超时时间

```properties
# 快速模型：减少超时
nanobot.llm.fallback.timeout-ms=10000

# 慢速模型：增加超时
nanobot.llm.fallback.timeout-ms=60000
```

### 3. 优化 Prompt

小模型对 Prompt 格式敏感：

```java
// ✅ 好的 Prompt（简洁明确）
"Calculate 100 + 200"

// ❌ 差的 Prompt（过于复杂）
"Please help me perform a mathematical calculation involving the addition of two numbers, specifically one hundred and two hundred"
```

### 4. 使用量化模型

```bash
# Q4 量化（推荐）
ollama pull llama3.2:1b-q4

# Q8 量化（更高质量）
ollama pull llama3.2:1b-q8
```

## 成本对比

### 本地模型 vs 云端模型

| 场景 | 本地模型 | 云端模型 |
|------|----------|----------|
| 硬件成本 | 一次性（GPU/CPU） | 无 |
| 推理成本 | 免费 | 按 token 计费 |
| 延迟 | 低（本地） | 中等（网络） |
| 质量 | 中等 | 高 |
| 隐私 | 完全私有 | 数据上传 |

### 成本估算

假设每天 10,000 次请求，每次 500 tokens：

**云端模型（GPT-4）：**
- 成本：10,000 × 500 × $0.00003 = $150/天
- 月成本：~$4,500

**本地模型（BitNet）：**
- 硬件成本：$500-2000（一次性）
- 电费：~$5/月
- 月成本：~$5

**节省：99% 以上**

## 故障排查

### 问题 1：Ollama 连接失败

**现象：**
```
Connection refused: localhost:11434
```

**解决：**
```bash
# 检查 Ollama 是否运行
ollama list

# 启动 Ollama
ollama serve

# 检查端口
lsof -i :11434
```

### 问题 2：模型响应慢

**现象：**
- 频繁触发降级
- 超时错误

**解决：**
1. 使用更小的模型
2. 增加超时时间
3. 检查 CPU/GPU 负载

### 问题 3：响应质量差

**现象：**
- 回答不准确
- 格式错误

**解决：**
1. 切换 Prompt 模板（chatML ↔ alpaca）
2. 调整 temperature
3. 使用更大的模型
4. 启用云端降级

## 监控和日志

### 关键日志

```
INFO  - Attempting to use primary model (Ollama)...
INFO  - Primary model succeeded in 1234ms
WARN  - Primary model timed out, falling back to cloud model
INFO  - Fallback triggered (reason: timeout). Total fallbacks: 5
INFO  - Fallback model succeeded in 2345ms
```

### 监控指标

- 本地模型成功率
- 降级触发次数
- 平均响应时间
- 成本节省比例

## 最佳实践

1. **开发环境**：使用本地模型，快速迭代
2. **生产环境**：启用降级策略，确保可用性
3. **成本优化**：大部分请求用本地，复杂任务用云端
4. **质量保证**：定期检查降级率，调整策略

## 下一步

1. 添加更多 Prompt 模板
2. 支持流式响应
3. 实现模型路由（根据任务复杂度选择模型）
4. 添加模型性能监控面板
