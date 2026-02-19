package com.nanobot.config;

import com.nanobot.entity.AgentConfig;
import com.nanobot.entity.ChatSession;
import com.nanobot.repository.AgentConfigRepository;
import com.nanobot.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据初始化配置
 *
 * 在应用启动时自动创建默认的 Agent 配置和测试会话
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(
            AgentConfigRepository agentConfigRepository,
            ChatSessionRepository sessionRepository) {
        return args -> {
            // 检查是否已经初始化
            if (agentConfigRepository.count() > 0) {
                log.info("Database already initialized, skipping...");
                return;
            }

            log.info("Initializing database with default data...");

            // 创建默认的通用助手 Agent
            AgentConfig generalAssistant = new AgentConfig();
            generalAssistant.setAgentId("general-assistant");
            generalAssistant.setName("通用助手");
            generalAssistant.setSystemPrompt("""
                You are a helpful AI assistant powered by Nanobot4J.
                You can help users with various tasks including:
                - Answering questions
                - Performing calculations
                - Providing information

                Be concise, helpful, and friendly in your responses.
                """);
            generalAssistant.setEnabled(true);
            generalAssistant.setContextWindowSize(100);
            generalAssistant.setToolIds("[]");
            generalAssistant.setCreatedAt(java.time.Instant.now());
            generalAssistant.setUpdatedAt(java.time.Instant.now());
            agentConfigRepository.save(generalAssistant);
            log.info("✓ Created Agent: general-assistant");

            // 创建数学助手 Agent
            AgentConfig mathAssistant = new AgentConfig();
            mathAssistant.setAgentId("math-assistant");
            mathAssistant.setName("数学助手");
            mathAssistant.setSystemPrompt("""
                You are a helpful math assistant.
                You can perform calculations and solve mathematical problems.
                Always show your work and explain your reasoning step by step.
                """);
            mathAssistant.setEnabled(true);
            mathAssistant.setContextWindowSize(50);
            mathAssistant.setToolIds("[\"calculator\"]");
            mathAssistant.setCreatedAt(java.time.Instant.now());
            mathAssistant.setUpdatedAt(java.time.Instant.now());
            agentConfigRepository.save(mathAssistant);
            log.info("✓ Created Agent: math-assistant");

            // 创建默认测试会话
            ChatSession defaultSession = new ChatSession("user123", "general-assistant", "test-user");
            sessionRepository.save(defaultSession);
            log.info("✓ Created default session: user123");

            log.info("Database initialization completed!");
        };
    }
}
