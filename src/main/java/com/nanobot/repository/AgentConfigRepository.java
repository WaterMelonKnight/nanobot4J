package com.nanobot.repository;

import com.nanobot.entity.AgentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AgentConfig Repository
 */
@Repository
public interface AgentConfigRepository extends JpaRepository<AgentConfig, Long> {

    Optional<AgentConfig> findByAgentId(String agentId);

    Optional<AgentConfig> findByAgentIdAndEnabled(String agentId, Boolean enabled);
}
