## 🎯 快速开始指南

### 1. 配置环境变量

```bash
# DeepSeek API Key
export DEEPSEEK_API_KEY="sk-your-deepseek-api-key"

# Kimi (Moonshot) API Key
export KIMI_API_KEY="sk-your-kimi-api-key"

# 可选：OpenAI API Key
export OPENAI_API_KEY="sk-your-openai-api-key"
```

### 2. 修改配置文件

编辑 `src/main/resources/application.yml`：

```yaml
nanobot:
  llm:
    # 设置默认模型
    default-model: ollama  # 可选: ollama, deepseek, kimi, openai

    models:
      # 启用/禁用特定模型
      deepseek:
        enabled: true
      kimi:
        enabled: true
      ollama:
        enabled: true
```

### 3. 使用示例

#### 方式 1: 使用默认模型

```java
@Service
public class MyService {
    @Autowired
    private LLMService llmService;

    public String chat(String userInput) {
        List<Message> messages = List.of(
            new Message.UserMessage(userInput)
        );

        Message.AssistantMessage response = llmService.chat(messages);
        return response.content();
    }
}
```

#### 方式 2: 使用 @UseModel 注解（推荐）

```java
@Service
public class CodeGenerationService {
    @Autowired
    private LLMService llmService;

    @UseModel("deepseek")
    public String generateJavaCode(String requirement) {
        // DeepSeek 擅长代码生成
        List<Message> messages = List.of(
            new Message.SystemMessage("You are a Java expert"),
            new Message.UserMessage(requirement)
        );

        return llmService.chat(messages).content();
    }

    @UseModel("kimi")
    public String analyzeLongDocument(String document) {
        // Kimi 支持长文本（8k tokens）
        List<Message> messages = List.of(
            new Message.UserMessage("Analyze this: " + document)
        );

        return llmService.chat(messages).content();
    }

    @UseModel("ollama")
    public String quickResponse(String query) {
        // Ollama 本地模型，响应快，无需 API Key
        List<Message> messages = List.of(
            new Message.UserMessage(query)
        );

        return llmService.chat(messages).content();
    }
}
```

#### 方式 3: 动态选择模型

```java
@Service
public class DynamicModelService {
    @Autowired
    private LLMService llmService;

    public String chatWithModel(String modelName, String userInput) {
        List<Message> messages = List.of(
            new Message.UserMessage(userInput)
        );

        // 运行时动态选择模型
        Message.AssistantMessage response =
            llmService.chatWithModel(modelName, messages);

        return response.content();
    }

    public void listAvailableModels() {
        Set<String> models = llmService.getAvailableModels();
        System.out.println("Available models: " + models);
        // 输出: Available models: [ollama, deepseek, kimi]
    }
}
```

### 4. 工具调用（Function Calling）

```java
@Service
public class ToolCallingService {
    @Autowired
    private LLMService llmService;

    @Autowired
    private ToolRegistry toolRegistry;

    @UseModel("deepseek")  // DeepSeek 支持 Function Calling
    public String chatWithTools(String userInput) {
        List<Message> messages = List.of(
            new Message.UserMessage(userInput)
        );

        // 获取所有可用工具
        List<Tool> tools = toolRegistry.getAllTools();

        // 调用 LLM（支持工具调用）
        Message.AssistantMessage response =
            llmService.chatWithTools(messages, tools);

        return response.content();
    }
}
```

### 5. 监控和统计

```java
@RestController
@RequestMapping("/api/llm")
public class LLMMonitorController {

    @Autowired
    private MultiModelLLMService llmService;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();

        llmService.getStats().forEach((model, stats) -> {
            Map<String, Object> modelStats = new HashMap<>();
            modelStats.put("successCount", stats.getSuccessCount());
            modelStats.put("failureCount", stats.getFailureCount());
            modelStats.put("averageDurationMs", stats.getAverageDurationMs());
            modelStats.put("successRate", stats.getSuccessRate());

            result.put(model, modelStats);
        });

        return result;
    }
}
```

输出示例：
```json
{
  "ollama": {
    "successCount": 150,
    "failureCount": 5,
    "averageDurationMs": 1200.5,
    "successRate": 0.9677
  },
  "deepseek": {
    "successCount": 80,
    "failureCount": 2,
    "averageDurationMs": 2500.3,
    "successRate": 0.9756
  },
  "kimi": {
    "successCount": 45,
    "failureCount": 1,
    "averageDurationMs": 3000.8,
    "successRate": 0.9783
  }
}
```

### 6. 降级策略测试

```java
@SpringBootTest
public class FallbackTest {

    @Autowired
    private LLMService llmService;

    @Test
    public void testFallback() {
        // 配置降级顺序: ollama -> deepseek -> kimi

        List<Message> messages = List.of(
            new Message.UserMessage("Hello!")
        );

        // 如果 Ollama 不可用，会自动降级到 DeepSeek
        Message.AssistantMessage response = llmService.chat(messages);

        assertNotNull(response);
        assertNotNull(response.content());
    }
}
```

### 7. 运行示例程序

```bash
# 编译项目
mvn clean package

# 运行应用
mvn spring-boot:run

# 或者直接运行 JAR
java -jar target/nanobot4j-1.0.0.jar
```

应用启动后，会自动运行 `MultiModelExample`，展示所有使用方式。

### 8. 常见问题

#### Q1: 如何禁用某个模型？
A: 在 `application.yml` 中设置 `enabled: false`：
```yaml
models:
  deepseek:
    enabled: false  # 禁用 DeepSeek
```

#### Q2: 如何调整超时时间？
A: 修改模型的 `timeout-ms` 配置：
```yaml
models:
  ollama:
    timeout-ms: 60000  # 60 秒
```

#### Q3: 如何关闭降级策略？
A: 设置 `fallback.enabled: false`：
```yaml
fallback:
  enabled: false
```

#### Q4: @UseModel 注解不生效？
A: 确保：
1. 方法是 `public` 的
2. 类被 Spring 管理（有 `@Service`、`@Component` 等注解）
3. 通过 Spring 注入调用，而不是直接 `new` 对象

#### Q5: 如何添加新的模型提供商？
A: 在 `application.yml` 中添加配置：
```yaml
models:
  my-custom-model:
    enabled: true
    provider: openai-compatible  # 如果兼容 OpenAI API
    base-url: https://api.example.com/v1
    api-key: ${MY_API_KEY}
    model: custom-model-name
```

### 9. 性能优化建议

1. **本地模型优先**：将 Ollama 设为默认模型，减少 API 调用成本
2. **合理设置超时**：根据模型特性调整超时时间
3. **启用降级策略**：确保高可用性
4. **监控统计信息**：定期查看各模型的成功率和响应时间

### 10. 生产环境部署

```yaml
# 生产环境配置示例
nanobot:
  llm:
    default-model: deepseek  # 生产环境使用云端模型

    models:
      deepseek:
        enabled: true
        timeout-ms: 30000

      kimi:
        enabled: true
        timeout-ms: 30000

      ollama:
        enabled: false  # 生产环境可能没有本地模型

    fallback:
      enabled: true
      order: [deepseek, kimi]  # 云端模型互为备份

# 日志级别
logging:
  level:
    com.nanobot.llm: INFO  # 生产环境降低日志级别
```

---

## 🎉 完成！

你现在拥有一个功能完整的多模型 LLM 架构。开始使用吧！

如有问题，请查看：
- [MULTI_MODEL_GUIDE.md](MULTI_MODEL_GUIDE.md) - 完整架构文档
- [ARCHITECTURE.md](ARCHITECTURE.md) - 系统架构说明
- [MultiModelExample.java](src/main/java/com/nanobot/example/MultiModelExample.java) - 示例代码
