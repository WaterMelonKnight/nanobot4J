package com.nanobot.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.domain.Message;
import com.nanobot.domain.ToolCall;
import com.nanobot.entity.ChatMessage;
import com.nanobot.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DatabaseMemory - 基于数据库的持久化 Memory 实现
 *
 * 设计思路：
 * 1. 所有消息都保存到数据库
 * 2. 支持上下文窗口管理（避免超出 token 限制）
 * 3. 使用事务确保数据一致性
 */
public class DatabaseMemory implements Memory {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMemory.class);

    private final String sessionId;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final int maxContextSize;
    private final AtomicInteger sequenceCounter;

    public DatabaseMemory(
            String sessionId,
            ChatMessageRepository messageRepository,
            ObjectMapper objectMapper,
            int maxContextSize) {
        this.sessionId = sessionId;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
        this.maxContextSize = maxContextSize;

        // 初始化序列号计数器
        List<ChatMessage> existingMessages = messageRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId);
        int maxSeq = existingMessages.stream()
            .mapToInt(ChatMessage::getSequenceNumber)
            .max()
            .orElse(0);
        this.sequenceCounter = new AtomicInteger(maxSeq);
    }

    @Override
    @Transactional
    public void addMessage(Message message) {
        ChatMessage entity = convertToEntity(message);
        messageRepository.save(entity);
        log.debug("Saved message to database: sessionId={}, role={}, messageId={}",
            sessionId, message.role(), message.id());
    }

    @Override
    @Transactional
    public void addMessages(List<Message> messages) {
        List<ChatMessage> entities = messages.stream()
            .map(this::convertToEntity)
            .toList();
        messageRepository.saveAll(entities);
        log.debug("Saved {} messages to database for session: {}", messages.size(), sessionId);
    }

    @Override
    public List<Message> getMessages() {
        List<ChatMessage> entities = messageRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId);
        return entities.stream()
            .map(this::convertToMessage)
            .toList();
    }

    @Override
    public List<Message> getRecentMessages(int count) {
        List<Message> allMessages = getMessages();
        if (allMessages.size() <= count) {
            return allMessages;
        }
        return allMessages.subList(allMessages.size() - count, allMessages.size());
    }

    @Override
    public List<Message> getContext() {
        List<Message> allMessages = getMessages();

        if (allMessages.size() <= maxContextSize) {
            return allMessages;
        }

        // 策略：保留所有系统消息 + 最近的消息
        List<Message> context = new ArrayList<>();
        List<Message> systemMessages = allMessages.stream()
            .filter(m -> m instanceof Message.SystemMessage)
            .toList();

        context.addAll(systemMessages);

        // 计算还能保留多少条非系统消息
        int remainingSlots = maxContextSize - systemMessages.size();
        if (remainingSlots > 0) {
            List<Message> nonSystemMessages = allMessages.stream()
                .filter(m -> !(m instanceof Message.SystemMessage))
                .toList();

            int startIndex = Math.max(0, nonSystemMessages.size() - remainingSlots);
            context.addAll(nonSystemMessages.subList(startIndex, nonSystemMessages.size()));
        }

        return Collections.unmodifiableList(context);
    }

    @Override
    @Transactional
    public void clear() {
        messageRepository.deleteBySessionId(sessionId);
        sequenceCounter.set(0);
        log.info("Cleared all messages for session: {}", sessionId);
    }

    @Override
    public int size() {
        return (int) messageRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId).size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * 将 Message 转换为数据库实体
     */
    private ChatMessage convertToEntity(Message message) {
        int sequence = sequenceCounter.incrementAndGet();
        ChatMessage entity = new ChatMessage(
            message.id(),
            sessionId,
            message.role(),
            null,
            sequence
        );

        if (message instanceof Message.UserMessage m) {
            entity.setContent(m.content());
        } else if (message instanceof Message.SystemMessage m) {
            entity.setContent(m.content());
        } else if (message instanceof Message.AssistantMessage m) {
            entity.setContent(m.content());
            if (m.toolCalls() != null && !m.toolCalls().isEmpty()) {
                try {
                    entity.setToolCalls(objectMapper.writeValueAsString(m.toolCalls()));
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize tool calls", e);
                }
            }
        } else if (message instanceof Message.ToolResultMessage m) {
            entity.setContent(m.result());
            entity.setToolCallId(m.toolCallId());
            entity.setToolName(m.toolName());
        }

        return entity;
    }

    /**
     * 将数据库实体转换为 Message
     */
    private Message convertToMessage(ChatMessage entity) {
        String role = entity.getRole();

        if ("user".equals(role)) {
            return new Message.UserMessage(
                entity.getMessageId(),
                entity.getContent(),
                entity.getCreatedAt()
            );
        } else if ("system".equals(role)) {
            return new Message.SystemMessage(
                entity.getMessageId(),
                entity.getContent(),
                entity.getCreatedAt()
            );
        } else if ("assistant".equals(role)) {
            List<ToolCall> toolCalls = parseToolCalls(entity.getToolCalls());
            return new Message.AssistantMessage(
                entity.getMessageId(),
                entity.getContent(),
                toolCalls,
                entity.getCreatedAt()
            );
        } else if ("tool".equals(role)) {
            return new Message.ToolResultMessage(
                entity.getMessageId(),
                entity.getToolCallId(),
                entity.getToolName(),
                entity.getContent(),
                true,
                entity.getCreatedAt()
            );
        } else {
            throw new IllegalArgumentException("Unknown message role: " + entity.getRole());
        }
    }

    /**
     * 解析工具调用 JSON
     */
    private List<ToolCall> parseToolCalls(String toolCallsJson) {
        if (toolCallsJson == null || toolCallsJson.isEmpty()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                toolCallsJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, ToolCall.class)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to parse tool calls JSON", e);
            return List.of();
        }
    }
}
