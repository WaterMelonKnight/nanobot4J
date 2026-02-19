package com.nanobot.example;

import com.nanobot.agent.Agent;
import com.nanobot.agent.BaseAgent;
import com.nanobot.domain.AgentResponse;
import com.nanobot.llm.LLMClient;
import com.nanobot.memory.InMemoryMemory;
import com.nanobot.memory.Memory;
import com.nanobot.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 示例应用 - 演示如何使用 Nanobot4J
 */
@Component
public class ExampleRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ExampleRunner.class);

    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;

    public ExampleRunner(LLMClient llmClient, ToolRegistry toolRegistry) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void run(String... args) {
        log.info("=== Nanobot4J Example ===");

        // 创建 Memory
        Memory memory = new InMemoryMemory(50);

        // 创建 Agent
        Agent agent = new BaseAgent(
            "MathAssistant",
            memory,
            llmClient,
            toolRegistry,
            """
            You are a helpful math assistant.
            You can perform calculations using the calculator tool.
            Always explain your reasoning step by step.
            """
        );

        // 初始化 Agent
        agent.initialize();

        // 示例 1：简单计算
        runExample(agent, "What is 123 + 456?");

        // 示例 2：多步计算
        runExample(agent, "Calculate (100 * 5) + (200 / 4)");

        // 示例 3：对话式交互
        runExample(agent, "I have 10 apples, I give away 3, then buy 5 more. How many do I have?");

        log.info("=== Examples completed ===");
    }

    private void runExample(Agent agent, String question) {
        log.info("\n--- Question: {} ---", question);

        try {
            AgentResponse response = agent.chat(question);

            log.info("State: {}", response.state());
            log.info("Iterations: {}", response.iterationCount());
            log.info("Answer: {}", response.content());

            if (response.hasError()) {
                log.error("Agent encountered an error");
            }
        } catch (Exception e) {
            log.error("Error running example", e);
        }
    }
}
