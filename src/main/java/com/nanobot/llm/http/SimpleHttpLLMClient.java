package com.nanobot.llm.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nanobot.config.MultiModelProperties;
import com.nanobot.domain.Message;
import com.nanobot.llm.LLMClient;
import com.nanobot.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 简单的 HTTP LLM 客户端 - 使用 Java 11+ HttpClient
 * 支持 OpenAI 兼容的 API
 */
public class SimpleHttpLLMClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(SimpleHttpLLMClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String modelName;
    private final MultiModelProperties.ModelConfig config;
    private final HttpClient httpClient;

    public SimpleHttpLLMClient(String modelName, MultiModelProperties.ModelConfig config) {
        this.modelName = modelName;
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    @Override
    public Message.AssistantMessage chat(List<Message> messages) {
        return chatWithTools(messages, List.of());
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public Message.AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools) {
        try {
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            // 转换消息
            ArrayNode messagesArray = objectMapper.createArrayNode();
            for (Message message : messages) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                if (message instanceof Message.UserMessage m) {
                    msgNode.put("role", "user");
                    msgNode.put("content", m.content());
                } else if (message instanceof Message.AssistantMessage m) {
                    msgNode.put("role", "assistant");
                    msgNode.put("content", m.content());
                } else if (message instanceof Message.SystemMessage m) {
                    msgNode.put("role", "system");
                    msgNode.put("content", m.content());
                }
                messagesArray.add(msgNode);
            }
            requestBody.set("messages", messagesArray);

            // 发送请求
            String endpoint = config.getBaseUrl() + "/chat/completions";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            log.debug("Sending request to {} with model {}", endpoint, config.getModel());

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("API request failed with status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("API request failed: " + response.statusCode());
            }

            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.body());
            String content = responseJson.path("choices").get(0).path("message").path("content").asText();

            return new Message.AssistantMessage(
                    "msg-" + System.currentTimeMillis(),
                    content,
                    List.of(),
                    Instant.now()
            );

        } catch (IOException | InterruptedException e) {
            log.error("Error calling LLM API", e);
            throw new RuntimeException("Failed to call LLM API: " + e.getMessage(), e);
        }
    }
}
