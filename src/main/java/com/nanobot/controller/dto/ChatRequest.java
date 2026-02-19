package com.nanobot.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 聊天请求 DTO
 */
@Schema(description = "聊天请求")
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户消息", example = "你好，请问今天天气怎么样？")
    private String message;

    @NotBlank(message = "会话 ID 不能为空")
    @Schema(description = "会话 ID", example = "user123")
    private String sessionId;

    public ChatRequest() {
    }

    public ChatRequest(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
