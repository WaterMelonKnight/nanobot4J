package com.nanobot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置属性
 */
@Configuration
@ConfigurationProperties(prefix = "nanobot.agent")
public class AgentProperties {

    /**
     * 默认最大迭代次数
     */
    private int maxIterations = 10;

    /**
     * 默认上下文窗口大小
     */
    private int contextWindowSize = 100;

    /**
     * 是否启用工具调用日志
     */
    private boolean enableToolLogging = true;

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getContextWindowSize() {
        return contextWindowSize;
    }

    public void setContextWindowSize(int contextWindowSize) {
        this.contextWindowSize = contextWindowSize;
    }

    public boolean isEnableToolLogging() {
        return enableToolLogging;
    }

    public void setEnableToolLogging(boolean enableToolLogging) {
        this.enableToolLogging = enableToolLogging;
    }
}
