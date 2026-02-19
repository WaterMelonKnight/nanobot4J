package com.nanobot.repository;

import com.nanobot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ChatSession Repository
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionId(String sessionId);

    Optional<ChatSession> findBySessionIdAndActive(String sessionId, Boolean active);
}
