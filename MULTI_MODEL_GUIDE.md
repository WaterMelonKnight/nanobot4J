# Nanobot4J - 多模型架构重构完成

## 📋 架构概览

我已经为你完成了 Nanobot4J 的多模型并发配置重构。新架构支持同时配置和使用多个 LLM 提供商（DeepSeek、Kimi、Ollama 等）。

## 🏗️ 核心组件

### 1. 配置层 (Configuration Layer)

#### `MultiModelProperties.java`
- 使用 `@ConfigurationProperties` 绑定配置
- 支持配置多个模型提供商
- 每个模型独立配置：API Key、Base URL、温度、超时等
- 支持降级策略配置

#### `application.yml`
```yaml
nanobot:
  llm:
    default-model: ollama
    models:
      deepseek:
        enabled: true
        provider: openai-compatible
        base-url: https://api.deepseek.com/v1
        api-key: ${DEEPSEEK_API_KEY}
        model: deepseek-chat
      kimi:
        enabled: true
        provider: openai-compatible
        base-url: https://api.moonshot.cn/v1
        api-key: ${KIMI_API_KEY}
        model: moonshot-v1-8k
      ollama:
        enabled: true
        provider: ollama
        base-url: http://localhost:11434
        model: bitnet
    fallback:
      enabled: true
      order: [ollama, deepseek, kimi]
```

### 2. 工厂层 (Factory Layer)

#### `ModelProviderFactory.java`
- **工厂模式 + 策略模式**
- 根据配置动态创建不同的 LLMClient 实例
- 支持通过名称获取客户端：`factory.getClient("deepseek")`
- 管理所有模型客户端的生命周期

### 3. 客户端层 (Client Layer)

#### `OpenAICompatibleLLMClient.java`
- 支持所有 OpenAI API 兼容的提供商
- DeepSeek、Kimi、OpenAI 都使用这个客户端
- 支持原生 Function Calling

#### `OllamaLLMClient.java`
- 专门用于 Ollama 本地模型
- 使用 PromptTemplate 格式化消息（适配小模型）

### 4. 服务层 (Service Layer)

#### `LLMService.java` (接口)
统一的 LLM 交互接口：
```java
public interface LLMService {
    Message.AssistantMessage chat(List<Message> messages);
    Message.AssistantMessage chatWithModel(String modelName, List<Message> messages);
    Set<String> getAvailableModels();
}
```

#### `MultiModelLLMService.java` (实现)
- 支持动态选择模型
- 自动降级策略（主模型失败时切换到备用模型）
- 超时控制（使用虚拟线程）
- 调用统计信息（成功率、平均响应时间）

### 5. 注解层 (Annotation Layer)

#### `@UseModel` 注解
```java
@UseModel("deepseek")
public String generateCode(String prompt) {
    // 这个方法会使用 DeepSeek 模型
}
```

#### `ModelSelectionAspect.java`
- AOP 切面，拦截 @UseModel 注解
- 使用 ThreadLocal 存储模型选择
- 自动清理上下文

## 🎯 使用方式

### 方式 1: 使用默认模型
```java
@Autowired
private LLMService llmService;

public void example1() {
    List<Message> messages = List.of(
        new Message.UserMessage("Hello!")
    );
    Message.AssistantMessage response = llmService.chat(messages);
}
```

### 方式 2: 使用 @UseModel 注解
```java
@Service
public class MyService {
    @Autowired
    private LLMService llmService;

    @UseModel("deepseek")
    public String generateCode() {
        // 自动使用 DeepSeek 模型
        return llmService.chat(messages).content();
    }

    @UseModel("kimi")
    public String analyzeText() {
        // 自动使用 Kimi 模型
        return llmService.chat(messages).content();
    }
}
```

### 方式 3: 动态选择模型
```java
public void example3() {
    String modelName = "deepseek"; // 运行时决定
    Message.AssistantMessage response =
        llmService.chatWithModel(modelName, messages);
}
```

### 方式 4: 查看可用模型
```java
Set<String> models = llmService.getAvailableModels();
// 输出: [ollama, deepseek, kimi]
```

## 🔄 降级策略

当主模型失败时，自动切换到备用模型：

```yaml
fallback:
  enabled: true
  order: [ollama, deepseek, kimi]
```

执行流程：
1. 优先使用 Ollama（本地模型，快速）
2. 如果 Ollama 超时或失败，切换到 DeepSeek
3. 如果 DeepSeek 也失败，切换到 Kimi
4. 所有模型都失败时抛出异常

## 📊 监控统计

```java
@Autowired
private MultiModelLLMService llmService;

public void showStats() {
    var stats = llmService.getStats();
    stats.forEach((model, stat) -> {
        System.out.printf("Model: %s, Success: %d, Failure: %d, Avg: %.2fms%n",
            model,
            stat.getSuccessCount(),
            stat.getFailureCount(),
            stat.getAverageDurationMs());
    });
}
```

## 🎨 设计模式

1. **工厂模式**: `ModelProviderFactory` 创建不同的客户端
2. **策略模式**: 不同的 LLMClient 实现不同的调用策略
3. **模板方法模式**: `PromptTemplate` 格式化消息
4. **AOP**: `@UseModel` 注解实现声明式模型选择
5. **降级模式**: 自动故障转移

## 🚀 下一步

1. **配置环境变量**:
```bash
export DEEPSEEK_API_KEY="your-deepseek-key"
export KIMI_API_KEY="your-kimi-key"
```

2. **启动应用**:
```bash
mvn spring-boot:run
```

3. **测试多模型**:
查看 `MultiModelExample.java` 中的示例代码

## 📁 新增文件清单

```
src/main/java/com/nanobot/
├── config/
│   ├── MultiModelProperties.java      # 多模型配置属性
│   └── MultiModelConfig.java          # 配置类
├── llm/
│   ├── service/
│   │   ├── LLMService.java            # 统一服务接口
│   │   └── MultiModelLLMService.java  # 多模型服务实现
│   ├── factory/
│   │   └── ModelProviderFactory.java  # 模型工厂
│   ├── openai/
│   │   └── OpenAICompatibleLLMClient.java  # OpenAI 兼容客户端
│   └── annotation/
│       ├── UseModel.java              # 模型选择注解
│       └── ModelSelectionAspect.java  # AOP 切面
├── example/
│   └── MultiModelExample.java         # 使用示例
└── resources/
    └── application.yml                # YAML 配置文件
```

架构重构完成！你现在拥有一个企业级的多模型 LLM 交互层。
