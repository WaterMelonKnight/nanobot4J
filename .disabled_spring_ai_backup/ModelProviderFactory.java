package com.nanobot.llm.factory;

import com.nanobot.config.MultiModelProperties;
import com.nanobot.llm.LLMClient;
import com.nanobot.llm.openai.OpenAICompatibleLLMClient;
import com.nanobot.llm.ollama.OllamaLLMClient;
import com.nanobot.llm.prompt.ChatMLPromptTemplate;
import com.nanobot.llm.prompt.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 模型提供商工厂
 *
 * 职责：
 * 1. 根据配置动态创建不同的 LLMClient 实例
 * 2. 管理所有模型客户端的生命周期
 * 3. 提供通过名称获取客户端的能力
 *
 * 设计模式：工厂模式 + 策略模式
 */
@Component
public class ModelProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(ModelProviderFactory.class);

    private final MultiModelProperties properties;
    private final Map<String, LLMClient> clientCache = new HashMap<>();
    private final PromptTemplate chatMLTemplate;

    public ModelProviderFactory(
            MultiModelProperties properties,
            @Qualifier("chatMLTemplate") PromptTemplate chatMLTemplate) {
        this.properties = properties;
        this.chatMLTemplate = chatMLTemplate;
        initializeClients();
    }

    /**
     * 初始化所有配置的客户端
     */
    private void initializeClients() {
        log.info("Initializing LLM clients from configuration...");

        properties.getModels().forEach((name, config) -> {
            if (Boolean.TRUE.equals(config.getEnabled())) {
                try {
                    LLMClient client = createClient(name, config);
                    clientCache.put(name, client);
                    log.info("✓ Initialized LLM client: {} (provider: {}, model: {})",
                            name, config.getProvider(), config.getModel());
                } catch (Exception e) {
                    log.error("✗ Failed to initialize LLM client: {}", name, e);
                }
            } else {
                log.info("⊘ Skipped disabled LLM client: {}", name);
            }
        });

        log.info("LLM client initialization complete. Total clients: {}", clientCache.size());
    }

    /**
     * 根据配置创建 LLMClient
     */
    private LLMClient createClient(String name, MultiModelProperties.ModelConfig config) {
        String provider = config.getProvider();

        return switch (provider) {
            case "openai-compatible" -> createOpenAICompatibleClient(name, config);
            case "ollama" -> createOllamaClient(name, config);
            case "anthropic" -> throw new UnsupportedOperationException("Anthropic provider not yet implemented");
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }

    /**
     * 创建 OpenAI 兼容的客户端（支持 DeepSeek, Kimi 等）
     */
    private LLMClient createOpenAICompatibleClient(String name, MultiModelProperties.ModelConfig config) {
        // 创建 OpenAI API 实例
        OpenAiApi openAiApi = new OpenAiApi(config.getBaseUrl(), config.getApiKey());

        // 创建 ChatModel
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(config.getModel())
                .withTemperature(config.getTemperature())
                .build();

        if (config.getMaxTokens() != null) {
            options = OpenAiChatOptions.builder()
                    .withModel(config.getModel())
                    .withTemperature(config.getTemperature())
                    .withMaxTokens(config.getMaxTokens())
                    .build();
        }

        OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, options);

        // 创建 ChatClient
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        return new OpenAICompatibleLLMClient(chatClient, name, config.getModel());
    }

    /**
     * 创建 Ollama 客户端
     */
    private LLMClient createOllamaClient(String name, MultiModelProperties.ModelConfig config) {
        // 创建 Ollama API 实例
        OllamaApi ollamaApi = new OllamaApi(config.getBaseUrl());

        // 创建 ChatModel
        OllamaOptions options = OllamaOptions.create()
                .withModel(config.getModel())
                .withTemperature(config.getTemperature().floatValue());

        OllamaChatModel chatModel = new OllamaChatModel(ollamaApi, options);

        // 创建 ChatClient
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // 选择 Prompt 模板
        PromptTemplate template = "chatML".equals(config.getPromptTemplate())
                ? chatMLTemplate
                : chatMLTemplate; // 可以扩展支持其他模板

        return new OllamaLLMClient(chatClient, template, config.getModel());
    }

    /**
     * 通过名称获取 LLMClient
     *
     * @param modelName 模型名称（如 "deepseek", "kimi", "ollama"）
     * @return LLMClient 实例
     * @throws IllegalArgumentException 如果模型不存在
     */
    public LLMClient getClient(String modelName) {
        LLMClient client = clientCache.get(modelName);
        if (client == null) {
            throw new IllegalArgumentException("LLM client not found: " + modelName +
                    ". Available clients: " + clientCache.keySet());
        }
        return client;
    }

    /**
     * 获取默认的 LLMClient
     */
    public LLMClient getDefaultClient() {
        String defaultModel = properties.getDefaultModel();
        return getClient(defaultModel);
    }

    /**
     * 尝试获取客户端（不抛出异常）
     */
    public Optional<LLMClient> tryGetClient(String modelName) {
        return Optional.ofNullable(clientCache.get(modelName));
    }

    /**
     * 获取所有可用的模型名称
     */
    public java.util.Set<String> getAvailableModels() {
        return clientCache.keySet();
    }

    /**
     * 检查模型是否可用
     */
    public boolean isModelAvailable(String modelName) {
        return clientCache.containsKey(modelName);
    }
}
