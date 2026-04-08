package com.nanobot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * 功能：
 * 1. 配置 RedisTemplate，使用 String 序列化器
 * 2. 配置 ObjectMapper 支持 Java 8 时间类型
 * 3. 支持条件化启用（通过配置文件控制）
 */
@Configuration
@ConditionalOnProperty(prefix = "nanobot.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    /**
     * 配置 RedisTemplate
     * 使用 String 序列化器，便于在 Redis 中查看和调试
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 String 序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 ObjectMapper（用于 JSON 序列化）
     * 注册 JavaTimeModule 以支持 Java 8 时间类型
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
