package com.nanobot.admin.controller;

import com.nanobot.admin.service.EnhancedAgentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 对话控制器 - 使用增强版服务
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final EnhancedAgentService agentService;

    /**
     * 处理用户消息 - 支持多步骤推理
     */
    @PostMapping("/chat")
    public EnhancedAgentService.AgentResponse chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getMessage());
        return agentService.chat(request.getMessage());
    }

    @Data
    public static class ChatRequest {
        private String message;
    }
}
