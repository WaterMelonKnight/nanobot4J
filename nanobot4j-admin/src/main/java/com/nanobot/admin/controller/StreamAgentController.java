package com.nanobot.admin.controller;

import com.nanobot.admin.service.StreamingGenericReActAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 流式 Agent 控制器 - 基于 SSE 和异步线程池
 */
@Slf4j
@RestController
@RequestMapping("/api/agent/stream")
@RequiredArgsConstructor
public class StreamAgentController {

    private final StreamingGenericReActAgent streamingAgent;

    // 存储活跃的 SSE 连接，用于监控和管理
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    // 使用缓存线程池处理异步任务（模拟虚拟线程的轻量级特性）
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("agent-stream-" + System.currentTimeMillis());
        return thread;
    });

    /**
     * 流式对话接口
     *
     * @param request 包含用户消息的请求体
     * @return SSE 流
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        String sessionId = generateSessionId();
        log.info("Starting streaming chat session: {}, message: {}", sessionId, request.message());

        // 创建 SSE Emitter，设置 5 分钟超时
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        activeEmitters.put(sessionId, emitter);

        // 设置完成和超时回调
        emitter.onCompletion(() -> {
            log.info("SSE session completed: ", sessionId);
            activeEmitters.remove(sessionId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE session timeout: {}", sessionId);
            activeEmitters.remove(sessionId);
            emitter.complete();
        });

        emitter.onError(throwable -> {
            log.error("SSE session error: {}", sessionId, throwable);
            activeEmitters.remove(sessionId);
        });

        // 使用线程池异步执行 ReAct 循环
        executorService.submit(() -> {
            try {
                streamingAgent.chatStreaming(request.message(), emitter);
            } catch (Exception e) {
                log.error("Error in streaming agent execution", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                    // Emitter 可能已经关闭
                }
            }
        });

        return emitter;
    }

    /**
     * 获取活跃连接数（监控用）
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return Map.of(
            "activeConnections", activeEmitters.size(),
            "threadPoolSize", ((java.util.concurrent.ThreadPoolExecutor) executorService).getPoolSize()
        );
    }

    /**
     * 生成会话 ID
     */
    private String generateSessionId() {
        return System.currentTimeMillis() + "-" +
               Integer.toHexString((int) (Math.random() * 0x1000000));
    }

    /**
     * 聊天请求
     */
    public record ChatRequest(String message) {}
}
