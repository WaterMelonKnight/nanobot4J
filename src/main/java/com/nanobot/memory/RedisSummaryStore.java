package com.nanobot.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 基于 Redis 的对话摘要存储实现
 *
 * 特性：
 * 1. 使用 Redis String 存储摘要文本
 * 2. 自动设置 TTL（与会话过期时间一致）
 * 3. 线程安全
 */
@Component
public class RedisSummaryStore implements SummaryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisSummaryStore.class);

    private static final String KEY_PREFIX = "chat:summary:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;

    public RedisSummaryStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveSummary(String sessionId, String summary) {
        try {
            String key = buildKey(sessionId);
            redisTemplate.opsForValue().set(key, summary, DEFAULT_TTL);
            log.debug("Saved summary for session {}: {} chars", sessionId, summary.length());
        } catch (Exception e) {
            log.error("Failed to save summary for session {}", sessionId, e);
            throw new RuntimeException("Failed to save summary", e);
        }
    }

    @Override
    public Optional<String> getSummary(String sessionId) {
        try {
            String key = buildKey(sessionId);
            String summary = redisTemplate.opsForValue().get(key);

            if (summary != null) {
                log.debug("Retrieved summary for session {}: {} chars", sessionId, summary.length());
                return Optional.of(summary);
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get summary for session {}", sessionId, e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteSummary(String sessionId) {
        try {
            String key = buildKey(sessionId);
            redisTemplate.delete(key);
            log.debug("Deleted summary for session {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to delete summary for session {}", sessionId, e);
        }
    }

    @Override
    public boolean hasSummary(String sessionId) {
        try {
            String key = buildKey(sessionId);
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check summary existence for session ", sessionId, e);
            return false;
        }
    }

    @Override
    public void refreshTTL(String sessionId) {
        try {
            String key = buildKey(sessionId);
            redisTemplate.expire(key, DEFAULT_TTL);
            log.debug("Refreshed TTL for summary of session {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to refresh TTL for summary of session {}", sessionId, e);
        }
    }

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
