package com.nanobot.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建会话请求 DTO
 */
public class CreateSessionRequest {

    @NotBlank(message = "Agent ID is required")
    private String agentId;

    private String userId;

    public CreateSessionRequest() {
    }

    public CreateSessionRequest(String agentId, String userId) {
        this.agentId = agentId;
        this.userId = userId;
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
}
