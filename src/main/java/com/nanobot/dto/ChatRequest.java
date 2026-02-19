package com.nanobot.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 聊天请求 DTO
 */
public class ChatRequest {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Message is required")
    private String message;

    private String userId;

    public ChatRequest() {
    }

    public ChatRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
