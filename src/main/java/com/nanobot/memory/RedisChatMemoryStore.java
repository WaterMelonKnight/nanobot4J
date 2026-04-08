package com.nanobot.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nanobot.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于 Redis 的企业级聊天记忆存储实现
 *
 * 特性：
 * 1. 使用 Redis List 存储消息列表
 * 2. 自动设置 TTL（24小时过期）
 * 3. 支持 JSON 序列化/反序列化
 * 4. 支持基于 token 的智能上下文管理
 * 5. 线程安全
 */
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryStore.class);

    private static final String KEY_PREFIX = "chat:session:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final int AVERAGE_TOKENS_PER_MESSAGE = 100; // 平均每条消息的 token 数

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        // 注册 JavaTimeModule 以支持 Java 8 时间类型
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void addMessage(String sessionId, Message message) {
        try {
            String key = buildKey(sessionId);
            String messageJson = objectMapper.writeValueAsString(message);

            // 将消息添加到 Redis List 的右侧（最新消息在右边）
            redisTemplate.opsForList().rightPush(key, messageJson);

            // 设置或刷新 TTL
            redisTemplate.expire(key, DEFAULT_TTL);

            log.debug("Added message to session {}: {}", sessionId, message.role());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for session {}", sessionId, e);
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    @Override
    public void addMessages(String sessionId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        try {
            String key = buildKey(sessionId);
            List<String> messageJsonList = messages.stream()
                    .map(msg -> {
                        try {
                            return objectMapper.writeValueAsString(msg);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to serialize message", e);
                            throw new RuntimeException("Failed to serialize message", e);
                        }
                    })
                    .collect(Collectors.toList());

            // 批量添加消息
            redisTemplate.opsForList().rightPushAll(key, messageJsonList);

            // 设置或刷新 TTL
            redisTemplate.expire(key, DEFAULT_TTL);

            log.debug("Added {} messages to session {}", messages.size(), sessionId);
        } catch (Exception e) {
            log.error("Failed to add messages to session {}", sessionId, e);
            throw new RuntimeException("Failed to add messages", e);
        }
    }

    @Override
    public List<Message> getMessages(String sessionId, int maxTokens) {
        List<Message> allMessages = getAllMessages(sessionId);

        if (allMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // 估算消息数量（基于平均 token 数）
        int estimatedMessageCount = maxTokens / AVERAGE_TOKENS_PER_MESSAGE;

        // 从最新的消息开始获取
        int startIndex = Math.max(0, allMessages.size() - estimatedMessageCount);
        List<Message> result = allMessages.subList(startIndex, allMessages.size());

        log.debug("Retrieved {} messages for session {} (maxTokens: {})",
                result.size(), sessionId, maxTokens);

        return result;
    }

    @Override
    public List<Message> getAllMessages(String sessionId) {
        try {
            String key = buildKey(sessionId);

            // 获取所有消息
            List<String> messageJsonList = redisTemplate.opsForList().range(key, 0, -1);

            if (messageJsonList == null || messageJsonList.isEmpty()) {
                return Collections.emptyList();
            }

            // 反序列化消息
            List<Message> messages = messageJsonList.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, Message.class);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to deserialize message from session {}", sessionId, e);
                            return null;
                        }
                    })
                    .filter(msg -> msg != null)
                    .collect(Collectors.toList());

            log.debug("Retrieved {} messages for session {}", messages.size(), sessionId);

            return messages;
        } catch (Exception e) {
            log.error("Failed to get messages for session {}", sessionId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Message> getRecentMessages(String sessionId, int count) {
        try {
            String key = buildKey(sessionId);

            // 获取最近的 N 条消息（从右侧开始）
            long start = -count;
            List<String> messageJsonList = redisTemplate.opsForList().range(key, start, -1);

            if (messageJsonList == null || messageJsonList.isEmpty()) {
                return Collections.emptyList();
            }

            // 反序列化消息
            List<Message> messages = messageJsonList.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, Message.class);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to deserialize message from session {}", sessionId, e);
                            return null;
                        }
                    })
                    .filter(msg -> msg != null)
                    .collect(Collectors.toList());

            log.debug("Retrieved {} recent messages for session {}", messages.size(), sessionId);

            return messages;
        } catch (Exception e) {
            log.error("Failed to get recent messages for session {}", sessionId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void clear(String sessionId) {
        try {
            String key = buildKey(sessionId);

            // 删除所有消息但保留 key（设置为空列表）
            redisTemplate.delete(key);

            log.debug("Cleared all messages for session {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to clear messages for session {}", sessionId, e);
            throw new RuntimeException("Failed to clear messages", e);
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        try {
            String key = buildKey(sessionId);
            redisTemplate.delete(key);

            log.debug("Deleted session {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to delete session {}", sessionId, e);
            throw new RuntimeException("Failed to delete session", e);
        }
    }

    @Override
    public boolean exists(String sessionId) {
        try {
            String key = buildKey(sessionId);
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check if session {} exists", sessionId, e);
            return false;
        }
    }

    @Override
    public long getMessageCount(String sessionId) {
        try {
            String key = buildKey(sessionId);
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get message count for session {}", sessionId, e);
            return 0;
        }
    }

    @Override
    public void refreshTTL(String sessionId) {
        try {
            String key = buildKey(sessionId);
            redisTemplate.expire(key, DEFAULT_TTL);

            log.debug("Refreshed TTL for session {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to refresh TTL for session {}", sessionId, e);
        }
    }

    /**
     * 构建 Redis key
     */
    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
