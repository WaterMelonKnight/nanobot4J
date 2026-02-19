package com.nanobot.memory;

import com.nanobot.domain.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于内存的 Memory 实现
 *
 * 设计思路：
 * 1. 使用 CopyOnWriteArrayList 保证线程安全
 * 2. 支持配置最大上下文窗口大小
 * 3. 当消息过多时，保留系统消息和最近的消息
 */
@Component
public class InMemoryMemory implements Memory {

    private final List<Message> messages = new CopyOnWriteArrayList<>();
    private final int maxContextSize;

    public InMemoryMemory() {
        this(100); // 默认保留最近 100 条消息
    }

    public InMemoryMemory(int maxContextSize) {
        this.maxContextSize = maxContextSize;
    }

    @Override
    public void addMessage(Message message) {
        messages.add(message);
    }

    @Override
    public void addMessages(List<Message> newMessages) {
        messages.addAll(newMessages);
    }

    @Override
    public List<Message> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    @Override
    public List<Message> getRecentMessages(int count) {
        int size = messages.size();
        if (size <= count) {
            return getMessages();
        }
        return Collections.unmodifiableList(
            new ArrayList<>(messages.subList(size - count, size))
        );
    }

    @Override
    public List<Message> getContext() {
        if (messages.size() <= maxContextSize) {
            return getMessages();
        }

        // 策略：保留所有系统消息 + 最近的消息
        List<Message> context = new ArrayList<>();
        List<Message> systemMessages = messages.stream()
            .filter(m -> m instanceof Message.SystemMessage)
            .toList();

        context.addAll(systemMessages);

        // 计算还能保留多少条非系统消息
        int remainingSlots = maxContextSize - systemMessages.size();
        if (remainingSlots > 0) {
            List<Message> nonSystemMessages = messages.stream()
                .filter(m -> !(m instanceof Message.SystemMessage))
                .toList();

            int startIndex = Math.max(0, nonSystemMessages.size() - remainingSlots);
            context.addAll(nonSystemMessages.subList(startIndex, nonSystemMessages.size()));
        }

        return Collections.unmodifiableList(context);
    }

    @Override
    public void clear() {
        messages.clear();
    }

    @Override
    public int size() {
        return messages.size();
    }

    @Override
    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
