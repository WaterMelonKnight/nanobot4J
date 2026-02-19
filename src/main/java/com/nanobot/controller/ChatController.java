package com.nanobot.controller;

import com.nanobot.controller.dto.ChatRequest;
import com.nanobot.controller.dto.ChatResponse;
import com.nanobot.domain.AgentResponse;
import com.nanobot.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Chat Controller - 聊天接口
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Chat API", description = "聊天接口")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    @Operation(summary = "发送聊天消息", description = "向指定会话发送消息并获取 AI 回复")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        try {
            log.info("Received chat request: sessionId={}, message={}",
                    request.getSessionId(), request.getMessage());

            AgentResponse agentResponse = agentService.chat(
                    request.getSessionId(),
                    request.getMessage()
            );

            ChatResponse response = ChatResponse.success(
                    agentResponse.content(),
                    request.getSessionId()
            );

            log.info("Chat request completed: sessionId={}", request.getSessionId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ChatResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Chat request failed", e);
            return ResponseEntity.internalServerError()
                    .body(ChatResponse.error("处理消息时发生错误: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查服务是否正常运行")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
