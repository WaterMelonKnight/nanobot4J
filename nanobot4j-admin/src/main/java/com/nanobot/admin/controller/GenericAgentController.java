package com.nanobot.admin.controller;

import com.nanobot.admin.service.GenericReActAgent;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 通用 Agent 控制器 - 完全动态化，无硬编码
 */
@Slf4j
@RestController
@RequestMapping("/api/agent/generic")
@RequiredArgsConstructor
public class GenericAgentController {

    private final GenericReActAgent genericAgent;

    /**
     * 通用对话接口 - 支持任意工具
     */
    @PostMapping("/chat")
    public GenericReActAgent.AgentResponse chat(@RequestBody ChatRequest request) {
        log.info("Received generic chat request: {}", request.getMessage());
        return genericAgent.chat(request.getMessage());
    }

    @Data
    public static class ChatRequest {
        private String message;
    }
}
