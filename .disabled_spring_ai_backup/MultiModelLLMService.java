package com.nanobot.llm.service;

import com.nanobot.config.MultiModelProperties;
import com.nanobot.domain.Message;
import com.nanobot.llm.LLMClient;
import com.nanobot.llm.factory.ModelProviderFactory;
import com.nanobot.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 多模型 LLM 服务实现
 *
 * 特性：
 * 1. 支持动态选择模型
 * 2. 支持自动降级策略
 * 3. 记录调用统计信息
 * 4. 支持超时控制
 */
@Service
public class MultiModelLLMService implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(MultiModelLLMService.class);

    private final ModelProviderFactory factory;
    private final MultiModelProperties properties;
    private final ExecutorService executor;

    // 统计信息
    private final ConcurrentHashMap<String, ModelStats> statsMap = new ConcurrentHashMap<>();

    public MultiModelLLMService(
            ModelProviderFactory factory,
            MultiModelProperties properties) {
        this.factory = factory;
        this.properties = properties;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public Message.AssistantMessage chat(List<Message> messages) {
        return chatWithTools(messages, List.of());
    }

    @Override
    public Message.AssistantMessage chatWithTools(List<Message> messages, List<Tool> tools) {
        // 检查是否有 @UseModel 注解指定的模型
        String modelName = getModelNameFromContext();
        return chatWithModelAndTools(modelName, messages, tools);
    }

    /**
     * 从上下文中获取模型名称
     * 优先级：@UseModel 注解 > 默认配置
     */
    private String getModelNameFromContext() {
        // 检查 AOP 设置的 ThreadLocal
        if (com.nanobot.llm.annotation.ModelSelectionAspect.hasModelContext()) {
            var context = com.nanobot.llm.annotation.ModelSelectionAspect.getCurrentModelContext();
            log.debug("Using model from @UseModel annotation: {}", context.getModelName());
            return context.getModelName();
        }

        // 使用默认模型
        return properties.getDefaultModel();
    }

    @Override
    public Message.AssistantMessage chatWithModel(String modelName, List<Message> messages) {
        return chatWithModelAndTools(modelName, messages, List.of());
    }

    @Override
    public Message.AssistantMessage chatWithModelAndTools(
            String modelName,
            List<Message> messages,
            List<Tool> tools) {

        // 如果启用了降级策略，使用降级逻辑
        if (Boolean.TRUE.equals(properties.getFallback().getEnabled())) {
            return chatWithFallback(modelName, messages, tools);
        }

        // 否则直接调用指定模型
        return callModel(modelName, messages, tools);
    }

    @Override
    public String getCurrentModelName() {
        return properties.getDefaultModel();
    }

    @Override
    public Set<String> getAvailableModels() {
        return factory.getAvailableModels();
    }

    /**
     * 带降级策略的调用
     */
    private Message.AssistantMessage chatWithFallback(
            String primaryModel,
            List<Message> messages,
            List<Tool> tools) {

        List<String> fallbackOrder = properties.getFallback().getOrder();

        // 确保主模型在降级列表的第一位
        if (!fallbackOrder.isEmpty() && !fallbackOrder.get(0).equals(primaryModel)) {
            fallbackOrder = new java.util.ArrayList<>(fallbackOrder);
            fallbackOrder.remove(primaryModel);
            fallbackOrder.add(0, primaryModel);
        }

        Exception lastException = null;

        for (String modelName : fallbackOrder) {
            if (!factory.isModelAvailable(modelName)) {
                log.warn("Model {} is not available, skipping", modelName);
                continue;
            }

            try {
                log.info("Attempting to use model: {}", modelName);
                Message.AssistantMessage response = callModel(modelName, messages, tools);

                if (!modelName.equals(primaryModel)) {
                    log.warn("Primary model {} failed, successfully fell back to {}",
                            primaryModel, modelName);
                }

                return response;

            } catch (Exception e) {
                log.error("Model {} failed: {}", modelName, e.getMessage());
                lastException = e;
                recordFailure(modelName);
            }
        }

        throw new RuntimeException("All models failed. Last error: " +
                (lastException != null ? lastException.getMessage() : "unknown"), lastException);
    }

    /**
     * 调用指定模型
     */
    private Message.AssistantMessage callModel(
            String modelName,
            List<Message> messages,
            List<Tool> tools) {

        Instant startTime = Instant.now();

        try {
            LLMClient client = factory.getClient(modelName);

            // 获取超时配置
            Long timeoutMs = properties.getModels().get(modelName).getTimeoutMs();

            // 带超时的调用
            Message.AssistantMessage response = callWithTimeout(
                    () -> client.chatWithTools(messages, tools),
                    timeoutMs
            );

            long duration = Duration.between(startTime, Instant.now()).toMillis();
            recordSuccess(modelName, duration);

            log.info("Model {} succeeded in {}ms", modelName, duration);
            return response;

        } catch (TimeoutException e) {
            log.error("Model {} timed out", modelName);
            throw new RuntimeException("Model " + modelName + " timed out", e);
        } catch (Exception e) {
            log.error("Model {} failed", modelName, e);
            throw new RuntimeException("Model " + modelName + " failed: " + e.getMessage(), e);
        }
    }

    /**
     * 带超时的调用
     */
    private Message.AssistantMessage callWithTimeout(
            Callable<Message.AssistantMessage> task,
            long timeoutMs) throws Exception {

        Future<Message.AssistantMessage> future = executor.submit(task);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }

    /**
     * 记录成功调用
     */
    private void recordSuccess(String modelName, long durationMs) {
        statsMap.computeIfAbsent(modelName, k -> new ModelStats())
                .recordSuccess(durationMs);
    }

    /**
     * 记录失败调用
     */
    private void recordFailure(String modelName) {
        statsMap.computeIfAbsent(modelName, k -> new ModelStats())
                .recordFailure();
    }

    /**
     * 获取统计信息
     */
    public ConcurrentHashMap<String, ModelStats> getStats() {
        return statsMap;
    }

    /**
     * 模型统计信息
     */
    public static class ModelStats {
        private long successCount = 0;
        private long failureCount = 0;
        private long totalDurationMs = 0;

        public synchronized void recordSuccess(long durationMs) {
            successCount++;
            totalDurationMs += durationMs;
        }

        public synchronized void recordFailure() {
            failureCount++;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public long getFailureCount() {
            return failureCount;
        }

        public double getAverageDurationMs() {
            return successCount == 0 ? 0.0 : (double) totalDurationMs / successCount;
        }

        public double getSuccessRate() {
            long total = successCount + failureCount;
            return total == 0 ? 0.0 : (double) successCount / total;
        }
    }
}
