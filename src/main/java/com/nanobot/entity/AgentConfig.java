package com.nanobot.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Agent 配置实体 - 保存 Agent 的配置信息
 */
@Entity
@Table(name = "agent_configs")
public class AgentConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String agentId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(columnDefinition = "TEXT")
    private String toolIds; // JSON 数组，例如：["calculator", "time_tool"]

    @Column(nullable = false)
    private Integer maxIterations = 10;

    @Column(nullable = false)
    private Integer contextWindowSize = 100;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public AgentConfig() {
    }

    public AgentConfig(String agentId, String name, String systemPrompt) {
        this.agentId = agentId;
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getToolIds() {
        return toolIds;
    }

    public void setToolIds(String toolIds) {
        this.toolIds = toolIds;
    }

    public Integer getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(Integer maxIterations) {
        this.maxIterations = maxIterations;
    }

    public Integer getContextWindowSize() {
        return contextWindowSize;
    }

    public void setContextWindowSize(Integer contextWindowSize) {
        this.contextWindowSize = contextWindowSize;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
