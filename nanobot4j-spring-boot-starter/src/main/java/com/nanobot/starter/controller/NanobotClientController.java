package com.nanobot.starter.controller;

import com.nanobot.core.tool.ToolResult;
import com.nanobot.starter.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Nanobot Client 执行端点
 * 接收来自 Admin 的远程工具调用请求
 */
@Slf4j
@RestController
@RequestMapping("/api/nanobot/client")
public class NanobotClientController {

    private final ToolRegistry toolRegistry;

    public NanobotClientController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 执行工具
     * @param request 包含 toolName 和 params
     * @return 标准响应格式
     */
    @PostMapping("/execute")
    public ToolExecutionResponse execute(@RequestBody ToolExecutionRequest request) {
        log.info("Received tool execution request: toolName={}, params={}",
                request.getToolName(), request.getParams());

        try {
            // 从注册表中获取工具并执行
            ToolResult result = toolRegistry.executeTool(request.getToolName(), request.getParams());

            if (result.isSuccess()) {
                log.info("Tool execution succeeded: toolName={}, result={}",
                        request.getToolName(), result.getData());
                return ToolExecutionResponse.success(result.getData());
            } else {
                log.error("Tool execution failed: toolName={}, error={}",
                        request.getToolName(), result.getError());
                return ToolExecutionResponse.failure(result.getError());
            }
        } catch (Exception e) {
            log.error("Tool execution exception: toolName={}", request.getToolName(), e);
            return ToolExecutionResponse.failure("Tool execution exception: " + e.getMessage());
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "toolCount", toolRegistry.getAllTools().size()
        );
    }
}
