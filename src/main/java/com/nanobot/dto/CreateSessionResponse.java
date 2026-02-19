package com.nanobot.dto;

/**
 * 创建会话响应 DTO
 */
public class CreateSessionResponse {

    private String sessionId;
    private String agentId;
    private String userId;
    private Long createdAt;

    public CreateSessionResponse() {
    }

    public CreateSessionResponse(String sessionId, String agentId, String userId, Long createdAt) {
        this.sessionId = sessionId;
        this.agentId = agentId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
