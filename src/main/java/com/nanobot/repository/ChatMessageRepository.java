package com.nanobot.repository;

import com.nanobot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ChatMessage Repository
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderBySequenceNumberAsc(String sessionId);

    void deleteBySessionId(String sessionId);
}
