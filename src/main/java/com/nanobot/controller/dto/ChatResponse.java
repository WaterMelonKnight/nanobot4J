package com.nanobot.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 聊天响应 DTO
 */
@Schema(description = "聊天响应")
public class ChatResponse {

    @Schema(description = "AI 回复内容", example = "你好！我是 Nanobot4J，很高兴为你服务。")
    private String reply;

    @Schema(description = "状态", example = "success")
    private String status;

    @Schema(description = "会话 ID", example = "user123")
    private String sessionId;

    @Schema(description = "错误消息（如果有）")
    private String error;

    public ChatResponse() {
    }

    public ChatResponse(String reply, String status, String sessionId, String error) {
        this.reply = reply;
        this.status = status;
        this.sessionId = sessionId;
        this.error = error;
    }

    public static ChatResponse success(String reply, String sessionId) {
        return new ChatResponse(reply, "success", sessionId, null);
    }

    public static ChatResponse error(String errorMessage) {
        return new ChatResponse(null, "error", null, errorMessage);
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
