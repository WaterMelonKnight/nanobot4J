package com.nanobot.dto;

import java.util.List;

/**
 * 聊天响应 DTO
 */
public class ChatResponse {

    private String sessionId;
    private String content;
    private String state;
    private Integer iterationCount;
    private Long timestamp;
    private List<MessageDTO> conversationHistory;

    public ChatResponse() {
    }

    public ChatResponse(String sessionId, String content, String state, Integer iterationCount) {
        this.sessionId = sessionId;
        this.content = content;
        this.state = state;
        this.iterationCount = iterationCount;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(Integer iterationCount) {
        this.iterationCount = iterationCount;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public List<MessageDTO> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<MessageDTO> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    /**
     * 消息 DTO
     */
    public static class MessageDTO {
        private String role;
        private String content;
        private Long timestamp;

        public MessageDTO() {
        }

        public MessageDTO(String role, String content, Long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
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

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
