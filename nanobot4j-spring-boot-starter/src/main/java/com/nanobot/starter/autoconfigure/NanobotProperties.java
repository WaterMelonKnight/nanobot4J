package com.nanobot.starter.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nanobot 配置属性
 */
@Data
@ConfigurationProperties(prefix = "nanobot")
public class NanobotProperties {

    /**
     * Admin 服务地址
     */
    private Admin admin = new Admin();

    /**
     * LLM 配置
     */
    private Llm llm = new Llm();

    @Data
    public static class Admin {
        /**
         * Admin 服务地址（例如：http://localhost:9090）
         */
        private String address = "http://localhost:9090";

        /**
         * 心跳间隔（秒）
         */
        private int heartbeatInterval = 30;

        /**
         * 是否启用注册
         */
        private boolean enabled = true;
    }

    @Data
    public static class Llm {
        /**
         * 默认模型
         */
        private String defaultModel = "deepseek";

        /**
         * DeepSeek API Key
         */
        private String deepseekApiKey;

        /**
         * DeepSeek Base URL
         */
        private String deepseekBaseUrl = "https://api.deepseek.com/v1";
    }
}
