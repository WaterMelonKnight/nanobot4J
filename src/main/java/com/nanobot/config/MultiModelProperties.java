package com.nanobot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多模型配置属性
 */
@Component
@ConfigurationProperties(prefix = "nanobot.llm")
public class MultiModelProperties {

    private String defaultModel = "deepseek";
    private Map<String, ModelConfig> models = new HashMap<>();
    private FallbackConfig fallback = new FallbackConfig();

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Map<String, ModelConfig> getModels() {
        return models;
    }

    public void setModels(Map<String, ModelConfig> models) {
        this.models = models;
    }

    public FallbackConfig getFallback() {
        return fallback;
    }

    public void setFallback(FallbackConfig fallback) {
        this.fallback = fallback;
    }

    public static class ModelConfig {
        private boolean enabled = true;
        private String provider;
        private String baseUrl;
        private String apiKey;
        private String model;
        private Double temperature = 0.7;
        private Integer maxTokens = 4096;
        private Long timeoutMs = 30000L;
        private String promptTemplate;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(Long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public String getPromptTemplate() {
            return promptTemplate;
        }

        public void setPromptTemplate(String promptTemplate) {
            this.promptTemplate = promptTemplate;
        }
    }

    public static class FallbackConfig {
        private boolean enabled = true;
        private List<String> order = List.of();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getOrder() {
            return order;
        }

        public void setOrder(List<String> order) {
            this.order = order;
        }
    }
}
