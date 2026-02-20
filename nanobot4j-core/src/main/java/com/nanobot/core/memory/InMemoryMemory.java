package com.nanobot.core.memory;

import com.nanobot.core.llm.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单的内存实现 - 存储在内存中
 */
public class InMemoryMemory implements Memory {

    private final List<Message> messages = new ArrayList<>();
    private final int maxSize;

    public InMemoryMemory() {
        this(100);
    }

    public InMemoryMemory(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void addMessage(Message message) {
        messages.add(message);
        // 保持最大容量
        while (messages.size() > maxSize) {
            messages.remove(0);
        }
    }

    @Override
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    @Override
    public void clear() {
        messages.clear();
    }

    @Override
    public List<Message> getRecentMessages(int n) {
        int size = messages.size();
        int start = Math.max(0, size - n);
        return new ArrayList<>(messages.subList(start, size));
    }
}
