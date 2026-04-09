package com.nanobot.admin.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.core.llm.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Redis 实现的聊天记忆存储
 *
 * 存储结构：
 * - Key: "chat:memory:{sessionId}"
 * - Type: List (LPUSH/LRANGE)
 * - TTL: 24 小时
 * - Value: JSON 序列化的 Message 对象
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "chat:memory:";
    private static final Duration TTL = Duration.ofHours(24);

    /**
     * 构建 Redis Key
     */
    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    @Override
    public void addMessage(String sessionId, Message message) {
        try {
            String key = buildKey(sessionId);
            String json = objectMapper.writeValueAsString(message);

            // 使用 RPUSH 追加到列表末尾（保持时间顺序）
            redisTemplate.opsForList().rightPush(key, json);

            // 刷新 TTL
            redisTemplate.expire(key, TTL);

            log.debug("Added message to session {}: role={}", sessionId, message.getRole());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message", e);
            throw new RuntimeException("Failed to add message to memory", e);
        }
    }

    @Override
    public List<Message> getMessages(String sessionId) {
        String key = buildKey(sessionId);

        // 获取所有消息（0 到 -1 表示全部）
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);

        if (jsonList == null || jsonList.isEmpty()) {
            return new ArrayList<>();
        }

        return jsonList.stream()
            .map(this::deserializeMessage)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> getRecentMessages(String sessionId, int limit) {
        String key = buildKey(sessionId);

        // 获取最后 N 条消息
        long count = getMessageCount(sessionId);
        long start = Math.max(0, count - limit);

        List<String> jsonList = redisTemplate.opsForList().range(key, start, -1);

        if (jsonList == null || jsonList.isEmpty()) {
            return new ArrayList<>();
        }

        return jsonList.stream()
            .map(this::deserializeMessage)
            .collect(Collectors.toList());
    }

    @Override
    public long getMessageCount(String sessionId) {
        String key = buildKey(sessionId);
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0;
    }

    @Override
    public void clearMessages(String sessionId) {
        String key = buildKey(sessionId);
        redisTemplate.delete(key);
        log.info("Cleared all messages for session {}", sessionId);
    }

    @Override
    public boolean exists(String sessionId) {
        String key = buildKey(sessionId);
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    /**
     * 反序列化消息
     */
    private Message deserializeMessage(String json) {
        try {
            return objectMapper.readValue(json, Message.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize message: {}", json, e);
            // 返回一个错误消息而不是抛出异常
            return Message.system("Error: Failed to load message from history");
        }
    }
}
