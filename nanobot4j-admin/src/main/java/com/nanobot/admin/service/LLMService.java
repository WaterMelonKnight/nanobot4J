package com.nanobot.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM服务 - 支持DeepSeek和Kimi API
 */
@Slf4j
@Service
public class LLMService {

    @Value("${llm.provider:deepseek}")
    private String provider;

    @Value("${llm.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${llm.kimi.api-key:}")
    private String kimiApiKey;

    @Value("${llm.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Value("${llm.kimi.model:moonshot-v1-8k}")
    private String kimiModel;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LLMService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用LLM生成响应
     */
    public String chat(String systemPrompt, String userMessage) {
        try {
            if ("deepseek".equalsIgnoreCase(provider)) {
                return callDeepSeek(systemPrompt, userMessage);
            } else if ("kimi".equalsIgnoreCase(provider)) {
                return callKimi(systemPrompt, userMessage);
            } else {
                log.warn("Unknown LLM provider: {}, falling back to DeepSeek", provider);
                return callDeepSeek(systemPrompt, userMessage);
            }
        } catch (Exception e) {
            log.error("LLM call failed", e);
            return "ERROR: LLM call failed - " + e.getMessage();
        }
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeek(String systemPrompt, String userMessage) throws Exception {
        log.info("Calling DeepSeek API with model: {}", deepseekModel);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepseekModel);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userMessage)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + deepseekApiKey)
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("DeepSeek API error: status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("DeepSeek API returned status " + response.statusCode());
        }

        JsonNode jsonResponse = objectMapper.readTree(response.body());
        String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();

        log.info("DeepSeek response: {}", content);
        return content;
    }

    /**
     * 调用Kimi API
     */
    private String callKimi(String systemPrompt, String userMessage) throws Exception {
        log.info("Calling Kimi API with model: {}", kimiModel);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", kimiModel);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userMessage)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.moonshot.cn/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + kimiApiKey)
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Kimi API error: status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("Kimi API returned status " + response.statusCode());
        }

        JsonNode jsonResponse = objectMapper.readTree(response.body());
        String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();

        log.info("Kimi response: {}", content);
        return content;
    }
}
