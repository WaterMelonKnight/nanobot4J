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
         * Admin 服务地址（例如：http://localhost:8080）
         */
        private String address = "http://localhost:8080";

        /**
         * 心跳间隔（秒）
         */
        private int heartbeatInterval = 30;

        /**
         * 是否启用注册
         */
        private boolean enabled = true;

        /**
         * 本实例对外暴露的回调地址（Admin 用此地址调用工具）
         * 不配置则自动推断为 http://localhost:{server.port}
         */
        private String instanceAddress;
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
