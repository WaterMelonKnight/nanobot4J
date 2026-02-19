package com.nanobot.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 消息实体 - 保存对话历史
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_session_id", columnList = "sessionId"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String messageId;

    @Column(nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false, length = 20)
    private String role; // user, assistant, system, tool

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String toolCalls; // JSON 格式存储工具调用

    @Column(length = 64)
    private String toolCallId;

    @Column(length = 128)
    private String toolName;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Integer sequenceNumber;

    public ChatMessage() {
    }

    public ChatMessage(String messageId, String sessionId, String role, String content, Integer sequenceNumber) {
        this.messageId = messageId;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.sequenceNumber = sequenceNumber;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(String toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
