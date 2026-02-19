package com.nanobot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.agent.Agent;
import com.nanobot.agent.BaseAgent;
import com.nanobot.domain.AgentResponse;
import com.nanobot.entity.AgentConfig;
import com.nanobot.entity.ChatSession;
import com.nanobot.llm.LLMClient;
import com.nanobot.memory.DatabaseMemory;
import com.nanobot.memory.Memory;
import com.nanobot.repository.AgentConfigRepository;
import com.nanobot.repository.ChatMessageRepository;
import com.nanobot.repository.ChatSessionRepository;
import com.nanobot.session.SessionManager;
import com.nanobot.tool.Tool;
import com.nanobot.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AgentService - 核心业务服务
 *
 * 职责：
 * 1. 管理多个 Agent 实例
 * 2. 处理会话创建和恢复
 * 3. 协调 SessionManager 进行并发控制
 * 4. 管理 Agent 和 Memory 的生命周期
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentConfigRepository agentConfigRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    // Agent 实例缓存 (agentId -> Agent)
    private final ConcurrentHashMap<String, Agent> agentCache = new ConcurrentHashMap<>();

    // Memory 实例缓存 (sessionId -> Memory)
    private final ConcurrentHashMap<String, Memory> memoryCache = new ConcurrentHashMap<>();

    public AgentService(
            AgentConfigRepository agentConfigRepository,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            LLMClient llmClient,
            ToolRegistry toolRegistry,
            SessionManager sessionManager,
            ObjectMapper objectMapper) {
        this.agentConfigRepository = agentConfigRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    /**
     * 处理聊天请求
     *
     * @param sessionId 会话 ID
     * @param userMessage 用户消息
     * @return Agent 响应
     */
    public AgentResponse chat(String sessionId, String userMessage) throws Exception {
        // 使用 SessionManager 确保并发安全
        return sessionManager.executeWithLock(sessionId, () -> {
            log.info("Processing chat request: sessionId=", sessionId);

            // 1. 获取或创建会话
            ChatSession session = getOrCreateSession(sessionId);

            // 2. 获取 Agent 实例
            Agent agent = getOrCreateAgent(session.getAgentId(), sessionId);

            // 3. 执行对话
            AgentResponse response = agent.chat(userMessage);

            // 4. 更新会话时间
            updateSessionTimestamp(session);

            log.info("Chat request completed: sessionId={}, iterations={}",
                sessionId, response.iterationCount());

            return response;
        });
    }

    /**
     * 创建新会话
     */
    @Transactional
    public ChatSession createSession(String agentId, String userId) {
        // 验证 Agent 配置存在
        AgentConfig config = agentConfigRepository.findByAgentIdAndEnabled(agentId, true)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found or disabled: " + agentId));

        String sessionId = UUID.randomUUID().toString();
        ChatSession session = new ChatSession(sessionId, agentId, userId);
        session = sessionRepository.save(session);

        log.info("Created new session: sessionId={}, agentId={}, userId={}",
            sessionId, agentId, userId);

        return session;
    }

    /**
     * 获取或创建会话
     */
    @Transactional
    private ChatSession getOrCreateSession(String sessionId) {
        return sessionRepository.findBySessionIdAndActive(sessionId, true)
            .orElseGet(() -> {
                // 会话不存在，自动创建一个新会话
                log.info("Session not found, creating new session: {}", sessionId);

                // 使用默认的 general-assistant
                String defaultAgentId = "general-assistant";
                AgentConfig config = agentConfigRepository.findByAgentIdAndEnabled(defaultAgentId, true)
                    .orElseGet(() -> {
                        // 如果 general-assistant 不存在，使用第一个可用的 agent
                        return agentConfigRepository.findAll().stream()
                            .filter(AgentConfig::getEnabled)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No enabled agent found"));
                    });

                ChatSession session = new ChatSession(sessionId, config.getAgentId(), "anonymous");
                return sessionRepository.save(session);
            });
    }

    /**
     * 获取或创建 Agent 实例
     */
    private Agent getOrCreateAgent(String agentId, String sessionId) {
        // 每个会话使用独立的 Agent 实例（共享配置，但独立 Memory）
        String cacheKey = agentId + ":" + sessionId;

        return agentCache.computeIfAbsent(cacheKey, k -> {
            log.debug("Creating new Agent instance: agentId={}, sessionId={}", agentId, sessionId);

            // 加载 Agent 配置
            AgentConfig config = agentConfigRepository.findByAgentIdAndEnabled(agentId, true)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

            // 创建 Memory
            Memory memory = getOrCreateMemory(sessionId, config.getContextWindowSize());

            // 加载工具
            List<Tool> tools = loadTools(config.getToolIds());

            // 创建 Agent
            Agent agent = new BaseAgent(
                config.getName(),
                memory,
                llmClient,
                toolRegistry,
                config.getSystemPrompt()
            );

            agent.initialize();

            return agent;
        });
    }

    /**
     * 获取或创建 Memory 实例
     */
    private Memory getOrCreateMemory(String sessionId, Integer contextWindowSize) {
        return memoryCache.computeIfAbsent(sessionId, k -> {
            log.debug("Creating new DatabaseMemory: sessionId={}", sessionId);
            int windowSize = contextWindowSize != null ? contextWindowSize : 100;
            return new DatabaseMemory(sessionId, messageRepository, objectMapper, windowSize);
        });
    }

    /**
     * 加载工具列表
     */
    private List<Tool> loadTools(String toolIdsJson) {
        if (toolIdsJson == null || toolIdsJson.isEmpty()) {
            return new ArrayList<>(toolRegistry.getAllTools());
        }

        try {
            List<String> toolIds = objectMapper.readValue(
                toolIdsJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            return toolIds.stream()
                .map(toolRegistry::getTool)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        } catch (Exception e) {
            log.error("Failed to parse tool IDs, using all tools", e);
            return new ArrayList<>(toolRegistry.getAllTools());
        }
    }

    /**
     * 更新会话时间戳
     */
    @Transactional
    private void updateSessionTimestamp(ChatSession session) {
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
    }

    /**
     * 清理会话缓存
     */
    public void clearSessionCache(String sessionId) {
        memoryCache.remove(sessionId);
        agentCache.entrySet().removeIf(entry -> entry.getKey().endsWith(":" + sessionId));
        sessionManager.cleanupSession(sessionId);
        log.info("Cleared cache for session: {}", sessionId);
    }

    /**
     * 获取会话信息
     */
    public Optional<ChatSession> getSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }

    /**
     * 关闭会话
     */
    @Transactional
    public void closeSession(String sessionId) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setActive(false);
            session.setUpdatedAt(Instant.now());
            sessionRepository.save(session);
            clearSessionCache(sessionId);
            log.info("Closed session: {}", sessionId);
        });
    }
}
