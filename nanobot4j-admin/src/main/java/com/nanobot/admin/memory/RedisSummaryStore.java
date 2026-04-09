package com.nanobot.admin.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 实现的摘要存储
 *
 * 存储结构：
 * - Key: "chat:summary:{sessionId}"
 * - Type: String
 * - TTL: 24 小时
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSummaryStore implements SummaryStore {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "chat:summary:";
    private static final Duration TTL = Duration.ofHours(24);

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    @Override
    public void saveSummary(String sessionId, String summary) {
        String key = buildKey(sessionId);
        redisTemplate.opsForValue().set(key, summary, TTL);
        log.info("Saved summary for session {}: {} chars", sessionId, summary.length());
    }

    @Override
    public String getSummary(String sessionId) {
        String key = buildKey(sessionId);
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean hasSummary(String sessionId) {
        String key = buildKey(sessionId);
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    @Override
    public void deleteSummary(String sessionId) {
        String key = buildKey(sessionId);
        redisTemplate.delete(key);
        log.info("Deleted summary for session {}", sessionId);
    }
}
