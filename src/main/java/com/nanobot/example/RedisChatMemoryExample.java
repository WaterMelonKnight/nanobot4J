package com.nanobot.example;

import com.nanobot.domain.Message;
import com.nanobot.memory.ChatMemoryStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RedisChatMemoryStore 使用示例
 *
 * 演示如何使用企业级记忆模块进行多会话管理
 */
@Component
@ConditionalOnProperty(name = "nanobot.example.redis-memory.enabled", havingValue = "true")
public class RedisChatMemoryExample implements CommandLineRunner {

    private final ChatMemoryStore chatMemoryStore;

    public RedisChatMemoryExample(ChatMemoryStore chatMemoryStore) {
        this.chatMemoryStore = chatMemoryStore;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n========== Redis Chat Memory Store 示例 ==========\n");

        // 示例 1: 基本的消息存储和检索
        example1_BasicUsage();

        // 示例 2: 多会话管理
        example2_MultiSession();

        // 示例 3: 基于 Token 的上下文管理
        example3_TokenBasedContext();

        // 示例 4: TTL 和会话管理
        example4_SessionManagement();

        System.out.println("\n========== 示例执行完成 ==========\n");
    }

    /**
     * 示例 1: 基本的消息存储和检索
     */
    private void example1_BasicUsage() {
        System.out.println("=== 示例 1: 基本的消息存储和检索 ===");

        String sessionId = "session-001";

        // 添加消息
        chatMemoryStore.addMessage(sessionId, new Message.UserMessage("你好，我是用户"));
        chatMemoryStore.addMessage(sessionId, new Message.AssistantMessage("你好！我是 AI 助手，有什么可以帮助你的吗？"));
        chatMemoryStore.addMessage(sessionId, new Message.UserMessage("请介绍一下 Java"));

        // 获取所有消息
        List<Message> messages = chatMemoryStore.getAllMessages(sessionId);
        System.out.println("会话 " + sessionId + " 共有 " + messages.size() + " 条消息");

        // 打印消息
        messages.forEach(msg -> System.out.println("  - " + msg.role() + ": " + getMessageContent(msg)));

        System.out.println();
    }

    /**
     * 示例 2: 多会话管理
     */
    private void example2_MultiSession() {
        System.out.println("=== 示例 2: 多会话管理 ===");

        // 会话 A: 讨论 Java
        String sessionA = "session-java";
        chatMemoryStore.addMessage(sessionA, new Message.UserMessage("Java 的特点是什么？"));
        chatMemoryStore.addMessage(sessionA, new Message.AssistantMessage("Java 是一种面向对象的编程语言..."));

        // 会话 B: 讨论 Python
        String sessionB = "session-python";
        chatMemoryStore.addMessage(sessionB, new Message.UserMessage("Python 的特点是什么？"));
        chatMemoryStore.addMessage(sessionB, new Message.AssistantMessage("Python 是一种简洁优雅的编程语言..."));

        // 检查会话是否存在
        System.out.println("会话 " + sessionA + " 存在: " + chatMemoryStore.exists(sessionA));
        System.out.println("会话 " + sessionB + " 存在: " + chatMemoryStore.exists(sessionB));

        // 获取消息数量
        System.out.println("会话 " + sessionA + " 消息数: " + chatMemoryStore.getMessageCount(sessionA));
        System.out.println("会话 " + sessionB + " 消息数: " + chatMemoryStore.getMessageCount(sessionB));

        System.out.println();
    }

    /**
     * 示例 3: 基于 Token 的上下文管理
     */
    private void example3_TokenBasedContext() {
        System.out.println("=== 示例 3: 基于 Token 的上下文管理 ===");

        String sessionId = "session-token-test";

        // 添加多条消息
        for (int i = 1; i <= 10; i++) {
            chatMemoryStore.addMessage(sessionId, new Message.UserMessage("用户消息 " + i));
            chatMemoryStore.addMessage(sessionId, new Message.AssistantMessage("助手回复 " + i));
        }

        // 获取所有消息
        List<Message> allMessages = chatMemoryStore.getAllMessages(sessionId);
        System.out.println("总消息数: " + allMessages.size());

        // 基于 token 限制获取消息（假设最多 500 tokens）
        List<Message> limitedMessages = chatMemoryStore.getMessages(sessionId, 500);
        System.out.println("限制 500 tokens 后的消息数: " + limitedMessages.size());

        // 获取最近 3 条消息
        List<Message> recentMessages = chatMemoryStore.getRecentMessages(sessionId, 3);
        System.out.println("最近 3 条消息:");
        recentMessages.forEach(msg -> System.out.println("  - " + msg.role() + ": " + getMessageContent(msg)));

        System.out.println();
    }

    /**
     * 示例 4: TTL 和会话管理
     */
    private void example4_SessionManagement() {
        System.out.println("=== 示例 4: TTL 和会话管理 ===");

        String sessionId = "session-ttl-test";

        // 添加消息
        chatMemoryStore.addMessage(sessionId, new Message.UserMessage("测试 TTL"));

        // 刷新 TTL
        chatMemoryStore.refreshTTL(sessionId);
        System.out.println("已刷新会话 " + sessionId + " 的 TTL");

        // 清空会话消息
        chatMemoryStore.clear(sessionId);
        System.out.println("已清空会话 " + sessionId + " 的消息");
        System.out.println("清空后消息数: " + chatMemoryStore.getMessageCount(sessionId));

        // 删除会话
        chatMemoryStore.deleteSession(sessionId);
        System.out.println("已删除会话 " + sessionId);
        System.out.println("会话是否存在: " + chatMemoryStore.exists(sessionId));

        System.out.println();
    }

    /**
     * 获取消息内容（辅助方法）
     */
    private String getMessageContent(Message message) {
        if (message instanceof Message.UserMessage msg) {
            return msg.content();
        } else if (message instanceof Message.AssistantMessage msg) {
            return msg.content();
        } else if (message instanceof Message.SystemMessage msg) {
            return msg.content();
        } else if (message instanceof Message.ToolResultMessage msg) {
            return msg.result();
        }
        return "";
    }
}
