package com.nanobot.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.admin.domain.AgentStreamEvent;
import com.nanobot.admin.domain.ServiceInstance;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流式泛型 ReAct Agent - 基于 SSE 实时推送
 *
 * 核心特性：
 * 1. 每个 ReAct 步骤实时推送事件到前端
 * 2. 使用虚拟线程处理长连接
 * 3. 完全动态化，零硬编码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingGenericReActAgent {

    private final InstanceRegistry instanceRegistry;
    private final RemoteToolExecutor remoteToolExecutor;
    private final LLMService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_ITERATIONS = 10;
    private static final String TOOL_CALL_MARKER = "TOOL_CALL:";
    private static final String FINAL_ANSWER_MARKER = "FINAL_ANSWER:";

    /**
     * 流式对话处理 - 主入口
     */
    public void chatStreaming(String userMessage, SseEmitter emitter) {
        log.info("Starting streaming ReAct for message: {}", userMessage);

        try {
            // 1. 获取所有在线工具
            List<ToolMetadata> availableTools = getAvailableTools();
            if (availableTools.isEmpty()) {
                sendEvent(emitter, AgentStreamEvent.error("当前没有可用的工具"));
                emitter.complete();
                return;
            }

            // 2. 初始化对话历史
            List<String> conversationHistory = new ArrayList<>();
            conversationHistory.add("User: " + userMessage);

            // 3. 开始 ReAct 循环
            sendEvent(emitter, AgentStreamEvent.thinking("🤔 开始分析任务..."));

            boolean taskCompleted = false;

            for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
                log.info("ReAct iteration {}/{}", iteration + 1, MAX_ITERATIONS);

                // 3.1 构建动态 Prompt
                String systemPrompt = buildDynamicPrompt(availableTools, conversationHistory);

                // 3.2 调用 LLM
                String currentMessage = (iteration == 0) ? userMessage :
                    "请根据上面的工具返回结果，继续分析并给出最终答案。";

                String llmResponse = llmService.chat(systemPrompt, currentMessage);
                log.info("LLM Response: {}", llmResponse);

                // 推送思考过程
                sendEvent(emitter, AgentStreamEvent.thinking("💭 " + llmResponse));

                // 3.3 解析 LLM 响应
                ParsedResponse parsed = parseLLMResponse(llmResponse);

                if (parsed.isFinalAnswer()) {
                    // 找到最终答案
                    sendEvent(emitter, AgentStreamEvent.finalAnswer(parsed.getAnswer()));
                    taskCompleted = true;
                    break;
                }

                if (parsed.isHasToolCall()) {
                    // 需要调用工具
                    ToolCall toolCall = parsed.getToolCall();

                    // 推送工具调用事件
                    String toolArgsJson = objectMapper.writeValueAsString(toolCall.getArguments());
                    sendEvent(emitter, AgentStreamEvent.toolCall(toolCall.getName(), toolArgsJson));

                    // 执行工具调用
                    String toolResult = executeToolCall(toolCall);

                    // 推送工具结果事件
                    sendEvent(emitter, AgentStreamEvent.toolResult(toolCall.getName(), toolResult));

                    // 更新对话历史
                    conversationHistory.add("Tool Call: " + toolCall.getName() +
                        " with args: " + toolCall.getArguments());
                    conversationHistory.add("Observation: " + toolResult);
                } else {
                    // LLM 没有明确指示
                    conversationHistory.add("Agent: " + llmResponse);
                }
            }

            // 4. 检查是否完成
            if (!taskCompleted) {
                sendEvent(emitter, AgentStreamEvent.error("达到最大迭代次数，无法完成任务"));
            }

            // 5. 推送完成事件
            sendEvent(emitter, AgentStreamEvent.done());
            emitter.complete();

        } catch (Exception e) {
            log.error("Error in streaming ReAct execution", e);
            try {
                sendEvent(emitter, AgentStreamEvent.error("执行出错: " + e.getMessage()));
                emitter.complete();
            } catch (Exception ignored) {
                // Emitter 可能已关闭
            }
        }
    }

    /**
     * 发送 SSE 事件
     */
    private void sendEvent(SseEmitter emitter, AgentStreamEvent event) {
        try {
            String jsonData = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event()
                .data(jsonData)
                .name("agent-event"));
            log.debug("Sent event: {}", event.type());
        } catch (IOException e) {
            log.error("Failed to send SSE event", e);
            throw new RuntimeException("Failed to send event", e);
        }
    }

    /**
     * 获取所有在线工具的元数据
     */
    private List<ToolMetadata> getAvailableTools() {
        List<ServiceInstance> onlineInstances = instanceRegistry.getOnlineInstances();
        List<ToolMetadata> tools = new ArrayList<>();

        for (ServiceInstance instance : onlineInstances) {
            if (instance.getTools() != null) {
                for (ServiceInstance.ToolInfo toolInfo : instance.getTools()) {
                    tools.add(new ToolMetadata(
                        toolInfo.getName(),
                        toolInfo.getDescription(),
                        toolInfo.getParameterSchema(),
                        instance.getAddress()
                    ));
                }
            }
        }

        log.info("Found {} available tools from {} online instances",
                 tools.size(), onlineInstances.size());
        return tools;
    }

    /**
     * 动态构建 Prompt
     */
    private String buildDynamicPrompt(List<ToolMetadata> tools, List<String> history) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个智能助手，可以使用以下工具来帮助用户：\n\n");

        // 动态注入工具列表
        prompt.append("可用工具：\n");
        for (ToolMetadata tool : tools) {
            prompt.append("- 工具名称: ").append(tool.getName()).append("\n");
            prompt.append("  描述: ").append(tool.getDescription()).append("\n");
            prompt.append("  参数格式: ").append(tool.getParameterSchema()).append("\n\n");
        }

        prompt.append("\n使用规则：\n");
        prompt.append("1. 仔细分析用户的请求\n");
        prompt.append("2. 如果需要使用工具，请严格按照以下格式输出（必须在一行内）：\n");
        prompt.append("   TOOL_CALL: {\"name\": \"工具名\", \"args\": {参数字典}}\n");
        prompt.append("3. 当你获得工具返回结果后，如果可以回答用户问题，请按以下格式输出最终答案：\n");
        prompt.append("   FINAL_ANSWER: 你的答案\n");
        prompt.append("4. 注意：每次只输出一个TOOL_CALL或FINAL_ANSWER，不要同时输出多个\n\n");

        // 添加对话历史
        if (!history.isEmpty()) {
            prompt.append("对话历史：\n");
            for (String entry : history) {
                prompt.append(entry).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("现在请分析用户的请求并决定下一步行动。");

        return prompt.toString();
    }

    /**
     * 解析 LLM 响应
     */
    private ParsedResponse parseLLMResponse(String response) {
        ParsedResponse parsed = new ParsedResponse();

        if (response.contains(FINAL_ANSWER_MARKER)) {
            String answer = response.substring(
                response.indexOf(FINAL_ANSWER_MARKER) + FINAL_ANSWER_MARKER.length()
            ).trim();
            parsed.setFinalAnswer(true);
            parsed.setAnswer(answer);
            return parsed;
        }

        if (response.contains(TOOL_CALL_MARKER)) {
            try {
                String jsonPart = response.substring(
                    response.indexOf(TOOL_CALL_MARKER) + TOOL_CALL_MARKER.length()
                ).trim();
                JsonNode node = objectMapper.readTree(jsonPart);

                ToolCall toolCall = new ToolCall();
                toolCall.setName(node.get("name").asText());

                Map<String, Object> args = new HashMap<>();
                JsonNode argsNode = node.get("args");
                if (argsNode != null) {
                    argsNode.fields().forEachRemaining(entry -> {
                        JsonNode valueNode = entry.getValue();
                        Object value;

                        // 根据 JSON 类型保留原始类型
                        if (valueNode.isNumber()) {
                            value = valueNode.numberValue();
                        } else if (valueNode.isBoolean()) {
                            value = valueNode.booleanValue();
                        } else if (valueNode.isNull()) {
                            value = null;
                        } else {
                            value = valueNode.asText();
                        }

                        args.put(entry.getKey(), value);
                    });
                }
                toolCall.setArguments(args);

                parsed.setHasToolCall(true);
                parsed.setToolCall(toolCall);
            } catch (Exception e) {
                log.error("Failed to parse tool call", e);
            }
        }

        return parsed;
    }

    /**
     * 执行工具调用
     */
    private String executeToolCall(ToolCall toolCall) {
        try {
            return remoteToolExecutor.executeRemoteTool(toolCall.getName(), toolCall.getArguments());
        } catch (Exception e) {
            log.error("Tool execution failed", e);
            return "Error: " + e.getMessage();
        }
    }

    // ========== 数据类 ==========

    @Data
    private static class ToolMetadata {
        private final String name;
        private final String description;
        private final String parameterSchema;
        private final String instanceAddress;
    }

    @Data
    private static class ToolCall {
        private String name;
        private Map<String, Object> arguments;
    }

    @Data
    private static class ParsedResponse {
        private boolean finalAnswer;
        private String answer;
        private boolean hasToolCall;
        private ToolCall toolCall;
    }
}
