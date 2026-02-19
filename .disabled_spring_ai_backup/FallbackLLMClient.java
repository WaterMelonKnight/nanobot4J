package com.nanobot.llm.fallback;

import com.nanobot.domain.Message;
import com.nanobot.llm.LLMClient;
import com.nanobot.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

/**
 * 模型降级策略 LLM 客户端
 *
 * 策略：
 * 1. 优先使用本地模型（Ollama）
 * 2. 如果本地模型超时或失败，自动切换到云端模型
 * 3. 记录降级事件，用于监控
 */
@Component("fallbackLLMClient")
public class FallbackLLMClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackLLMClient.class);

    private final LLMClient primaryClient;   // 本地模型（Ollama）
    private final LLMClient fallbackClient;  // 云端模型（OpenAI/DeepSeek）
    private final long timeoutMillis;
    private final ExecutorService executor;

    // 统计信息
    private long primarySuccessCount = 0;
    private long fallbackCount = 0;

    public FallbackLLMClient(
            @Qualifier("ollamaLLMClient") LLMClient primaryClient,
            @Qualifier("springAILLMClient") LLMClient fallbackClient) {
        this.primaryClient = primaryClient;
        this.fallbackClient = fallbackClient;
        this.timeoutMillis = 30000; // 30 秒超时
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public Message.AssistantMessage chat(List<Message> messages) {
        return chatWithTools(messages, List.of());
    }

    @Override
    public Message.AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools) {
        Instant startTime = Instant.now();

        try {
            // 尝试使用本地模型
            log.info("Attempting to use primary model (Ollama)...");
            Message.AssistantMessage response = callWithTimeout(
                () -> primaryClient.chatWithTools(messages, tools),
                timeoutMillis
            );

            primarySuccessCount++;
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            log.info("Primary model succeeded in {}ms", duration);

            return response;

        } catch (TimeoutException e) {
            log.warn("Primary model timed out after {}ms, falling back to cloud model", timeoutMillis);
            return fallbackToCloud(messages, tools, "timeout");

        } catch (Exception e) {
            log.error("Primary model failed: {}, falling back to cloud model", e.getMessage());
            return fallbackToCloud(messages, tools, "error: " + e.getMessage());
        }
    }

    @Override
    public String getModelName() {
        return String.format("%s (fallback: %s)",
            primaryClient.getModelName(),
            fallbackClient.getModelName());
    }

    /**
     * 降级到云端模型
     */
    private Message.AssistantMessage fallbackToCloud(
            List<Message> messages,
            List<Tool> tools,
            String reason) {
        fallbackCount++;
        log.info("Fallback triggered (reason: {}). Total fallbacks: {}", reason, fallbackCount);

        try {
            Instant startTime = Instant.now();
            Message.AssistantMessage response = fallbackClient.chatWithTools(messages, tools);
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            log.info("Fallback model succeeded in {}ms", duration);
            return response;

        } catch (Exception e) {
            log.error("Fallback model also failed", e);
            throw new RuntimeException("Both primary and fallback models failed", e);
        }
    }

    /**
     * 带超时的调用
     */
    private Message.AssistantMessage callWithTimeout(
            Callable<Message.AssistantMessage> task,
            long timeoutMillis) throws Exception {

        Future<Message.AssistantMessage> future = executor.submit(task);

        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }

    /**
     * 获取统计信息
     */
    public FallbackStats getStats() {
        return new FallbackStats(primarySuccessCount, fallbackCount);
    }

    /**
     * 统计信息记录
     */
    public record FallbackStats(long primarySuccessCount, long fallbackCount) {
        public double fallbackRate() {
            long total = primarySuccessCount + fallbackCount;
            return total == 0 ? 0.0 : (double) fallbackCount / total;
        }
    }
}
