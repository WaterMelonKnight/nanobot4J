package com.nanobot.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.admin.domain.AgentStreamEvent;
import com.nanobot.admin.domain.ServiceInstance;
import com.nanobot.admin.memory.ChatMemoryStore;
import com.nanobot.admin.memory.MemorySummarizer;
import com.nanobot.admin.tool.DynamicToolRegistry;
import com.nanobot.admin.tool.ToolCreatorTool;
import com.nanobot.core.llm.Message;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工业级流式 ReAct Agent - Phase 3 重构版
 *
 * 核心能力：
 * 1. 多级记忆：Redis 持久化 + 滑动窗口 + 异步摘要
 * 2. 动态工具自举：LLM 可通过 create_tool 在运行时创建新工具
 * 3. 熔断器：最大 15 步强制退出
 * 4. 防死循环：连续相同错误拦截 + 系统警告注入
 * 5. 强制 <thinking> 标签：SSE 实时解析并推送思考过程
 * 6. 全程 SSE 推流：所有中间状态实时推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingGenericReActAgent {

    private final InstanceRegistry instanceRegistry;
    private final RemoteToolExecutor remoteToolExecutor;
    private final LLMService llmService;
    private final ChatMemoryStore chatMemoryStore;
    private final MemorySummarizer memorySummarizer;
    private final DynamicToolRegistry dynamicToolRegistry;
    private final ToolCreatorTool toolCreatorTool;
    private final ObjectMapper objectMapper;

    // ========== 熔断器常量 ==========

    /** 最大执行步数（熔断阈值） */
    private static final int MAX_STEPS = 15;

    /** 滑动窗口大小 */
    private static final int MEMORY_WINDOW = 10;

    // ========== 协议标记 ==========

    private static final String TOOL_CALL_MARKER   = "TOOL_CALL:";
    private static final String FINAL_ANSWER_MARKER = "FINAL_ANSWER:";
    private static final Pattern THINKING_PATTERN =
        Pattern.compile("<thinking>(.*?)</thinking>", Pattern.DOTALL);

    // ========== 主入口 ==========

    /**
     * 流式对话 - 主入口
     *
     * @param sessionId   会话 ID（多轮记忆的 key）
     * @param userMessage 用户消息
     * @param emitter     SSE 推流器
     */
    public void chatStreaming(String sessionId, String userMessage, SseEmitter emitter) {
        log.info("[ReAct] session={}, message={}", sessionId, userMessage);

        try {
            // ── Phase 1: 加载记忆 ───────────────────────────────────────────
            List<Message> memoryHistory = memorySummarizer.applyWindow(sessionId);
            sendEvent(emitter, AgentStreamEvent.thinking(
                "📚 已加载 " + memoryHistory.size() + " 条历史记忆"));

            // ── Phase 2: 拉取可用工具（远程 + 动态） ──────────────────────
            List<ToolMetadata> availableTools = buildAvailableTools();
            if (availableTools.isEmpty()) {
                sendEvent(emitter, AgentStreamEvent.error("当前没有可用的工具"));
                emitter.complete();
                return;
            }
            sendEvent(emitter, AgentStreamEvent.thinking(
                "🔧 可用工具：" + availableTools.stream()
                    .map(ToolMetadata::getName).toList()));

            // ── Phase 3: 保存用户消息到 Redis ─────────────────────────────
            chatMemoryStore.addMessage(sessionId, Message.user(userMessage));

            // ── Phase 4: 构建当前轮次的对话上下文 ─────────────────────────
            List<String> roundHistory = new ArrayList<>();
            roundHistory.add("User: " + userMessage);

            // ── Phase 5: ReAct 主循环（含熔断器）─────────────────────────
            sendEvent(emitter, AgentStreamEvent.thinking("🤔 开始分析任务..."));

            // 防死循环状态跟踪
            String lastErrorKey   = null;
            int    repeatErrorCount = 0;

            boolean taskCompleted = false;

            for (int step = 0; step < MAX_STEPS; step++) {
                log.info("[ReAct] step={}/{}", step + 1, MAX_STEPS);

                // 3.1 构建 System Prompt
                String systemPrompt = buildSystemPrompt(availableTools, memoryHistory, roundHistory);

                // 3.2 调用 LLM
                String userTurn = (step == 0)
                    ? userMessage
                    : "请根据上面的工具返回结果，继续分析并给出最终答案。";

                String llmResponse = llmService.chat(systemPrompt, userTurn);
                log.debug("[ReAct] step={} llmResponse={}", step, llmResponse);

                // 3.3 解析并推送 <thinking> 标签
                String thinkingContent = extractThinking(llmResponse);
                if (!thinkingContent.isEmpty()) {
                    sendEvent(emitter, AgentStreamEvent.thinking("🧠 " + thinkingContent));
                }

                // 去掉 <thinking> 块，得到动作部分
                String actionPart = removeThinking(llmResponse);

                // 3.4 解析动作
                ParsedResponse parsed = parseLLMResponse(actionPart);

                // ── FINAL_ANSWER ─────────────────────────────────────────
                if (parsed.isFinalAnswer()) {
                    String answer = parsed.getAnswer();

                    // 保存 AI 回复到 Redis
                    chatMemoryStore.addMessage(sessionId, Message.assistant(answer));

                    // 异步触发记忆摘要（不阻塞 SSE）
                    memorySummarizer.summarizeIfNeeded(sessionId);

                    sendEvent(emitter, AgentStreamEvent.finalAnswer(answer));
                    taskCompleted = true;
                    break;
                }

                // ── TOOL_CALL ─────────────────────────────────────────────
                if (parsed.isHasToolCall()) {
                    ToolCall toolCall = parsed.getToolCall();
                    String toolArgsJson = objectMapper.writeValueAsString(toolCall.getArguments());

                    sendEvent(emitter, AgentStreamEvent.toolCall(toolCall.getName(), toolArgsJson));

                    // 执行工具（区分内建/动态/远程）
                    String toolResult = dispatchToolCall(toolCall);

                    // ── 防死循环检测 ──────────────────────────────────────
                    if (toolResult.startsWith("Error:")) {
                        String errorKey = toolCall.getName() + "|" + toolArgsJson;

                        if (errorKey.equals(lastErrorKey)) {
                            repeatErrorCount++;
                        } else {
                            lastErrorKey    = errorKey;
                            repeatErrorCount = 1;
                        }

                        if (repeatErrorCount >= 2) {
                            // 注入系统警告，强制 LLM 换策略
                            String sysWarning =
                                "【系统警告】你陷入了重复的错误执行路径（连续 " + repeatErrorCount +
                                " 次对工具 '" + toolCall.getName() +
                                "' 产生相同错误）。请立即使用完全不同的策略，" +
                                "或调用 create_tool 编写新工具，或直接向用户求助。";

                            sendEvent(emitter, AgentStreamEvent.warning(sysWarning));
                            toolResult = sysWarning;
                            repeatErrorCount = 0; // 重置，给 LLM 一次改正机会
                        }
                    } else {
                        // 执行成功，重置错误计数
                        lastErrorKey    = null;
                        repeatErrorCount = 0;
                    }

                    sendEvent(emitter, AgentStreamEvent.toolResult(toolCall.getName(), toolResult));

                    // 更新本轮历史和工具列表（动态工具可能新增）
                    roundHistory.add("Tool Call: " + toolCall.getName()
                        + " args=" + toolArgsJson);
                    roundHistory.add("Observation: " + toolResult);

                    // 刷新可用工具（create_tool 可能注册了新工具）
                    availableTools = buildAvailableTools();

                } else {
                    // LLM 输出了纯文本（中间思考），记入历史继续
                    roundHistory.add("Agent: " + actionPart);
                }
            }

            // ── 熔断器触发 ───────────────────────────────────────────────
            if (!taskCompleted) {
                String circuitMsg = "⚡ 熔断器触发：已达最大步数 " + MAX_STEPS +
                    " 步，强制退出。请简化任务或拆分后重试。";
                log.warn("[ReAct] circuit breaker triggered for session={}", sessionId);
                sendEvent(emitter, AgentStreamEvent.error(circuitMsg));
            }

            sendEvent(emitter, AgentStreamEvent.done());
            emitter.complete();

        } catch (Exception e) {
            log.error("[ReAct] fatal error for session={}", sessionId, e);
            try {
                sendEvent(emitter, AgentStreamEvent.error("执行出错: " + e.getMessage()));
                emitter.complete();
            } catch (Exception ignored) { }
        }
    }

    // ========== 工具分发（内建 → 动态 → 远程）==========

    /**
     * 工具调用分发器
     * 优先级：ToolCreatorTool > DynamicToolRegistry > RemoteToolExecutor
     */
    private String dispatchToolCall(ToolCall toolCall) {
        String name = toolCall.getName();
        Map<String, Object> args = toolCall.getArguments();

        try {
            // 1. 内建 ToolCreatorTool
            if (ToolCreatorTool.TOOL_NAME.equals(name)) {
                log.info("[ReAct] dispatching to ToolCreatorTool");
                return toolCreatorTool.execute(args);
            }

            // 2. 动态 Groovy 工具
            if (dynamicToolRegistry.hasTool(name)) {
                log.info("[ReAct] dispatching to DynamicGroovyTool: {}", name);
                return dynamicToolRegistry.executeTool(name, args);
            }

            // 3. 远程工具（SSE 实例）
            log.info("[ReAct] dispatching to RemoteToolExecutor: {}", name);
            return remoteToolExecutor.executeRemoteTool(name, args);

        } catch (Exception e) {
            log.error("[ReAct] tool execution error: tool={}", name, e);
            return "Error: " + e.getMessage();
        }
    }

    // ========== System Prompt 构建 ==========

    /**
     * 构建完整的 System Prompt
     *
     * 包含：
     * 1. Agent 角色定义 + 强制自举指令
     * 2. 记忆历史（滑动窗口 + 摘要）
     * 3. 工具列表（静态 + 动态）
     * 4. 协议规范（强制 <thinking> 标签）
     * 5. 熔断与防循环规则
     */
    private String buildSystemPrompt(
        List<ToolMetadata> tools,
        List<Message> memoryHistory,
        List<String> roundHistory
    ) {
        StringBuilder sb = new StringBuilder();

        // ── 1. 角色定义与核心原则 ─────────────────────────────────────────
        sb.append("""
            # 角色
            你是 Nanobot，一个拥有自我编程能力的工业级 ReAct Agent。

            # 核心原则
            1. 你必须逐步推理，每次只执行一个动作（TOOL_CALL 或 FINAL_ANSWER）。
            2. **严禁回答"我做不到"**。若现有工具无法完成任务，你必须调用 `create_tool`，
               用 Groovy 编写新工具并立即使用。
            3. 遇到工具报错时，必须先用 <thinking> 反思原因，再换策略，禁止无脑重试。

            """);

        // ── 2. 历史记忆（多轮上下文）─────────────────────────────────────
        if (!memoryHistory.isEmpty()) {
            sb.append("# 历史记忆（最近几轮对话摘要）\n");
            for (Message msg : memoryHistory) {
                String label = switch (msg.getRole()) {
                    case "system"    -> "【摘要】";
                    case "user"      -> "【用户】";
                    case "assistant" -> "【助手】";
                    default          -> "【" + msg.getRole() + "】";
                };
                sb.append(label).append(" ").append(msg.getContent()).append("\n");
            }
            sb.append("\n");
        }

        // ── 3. 可用工具列表 ───────────────────────────────────────────────
        sb.append("# 可用工具\n");
        for (ToolMetadata tool : tools) {
            sb.append("- **").append(tool.getName()).append("**：")
              .append(tool.getDescription()).append("\n");
            sb.append("  参数格式：").append(tool.getParameterSchema()).append("\n");
        }
        sb.append("\n");

        // ── 4. 严格输出协议 ───────────────────────────────────────────────
        sb.append("""
            # 输出协议（严格遵守）

            ## 每一步必须先输出思考
            在任何 TOOL_CALL 或 FINAL_ANSWER 之前，必须先输出：
            ```
            <thinking>
            [你的推理过程：分析当前状态、选择工具的理由、上一步错误的反思]
            </thinking>
            ```

            ## 调用工具
            ```
            TOOL_CALL: {"name": "工具名", "args": {参数字典}}
            ```

            ## 给出最终答案
            ```
            FINAL_ANSWER: 你的完整答案
            ```

            ## 禁止事项
            - 禁止在一次输出中同时出现多个 TOOL_CALL
            - 禁止省略 <thinking> 标签
            - 禁止输出"我无法..."、"我做不到..."等放弃语句

            """);

        // ── 5. 本轮对话历史 ───────────────────────────────────────────────
        if (roundHistory.size() > 1) { // 第一条是 User 消息，已在 userTurn 传入
            sb.append("# 本轮执行记录\n");
            for (int i = 1; i < roundHistory.size(); i++) {
                sb.append(roundHistory.get(i)).append("\n");
            }
            sb.append("\n");
        }

        sb.append("请根据以上信息，输出你的 <thinking> 和下一步动作。");

        return sb.toString();
    }

    // ========== 工具列表构建 ==========

    /**
     * 合并远程工具 + ToolCreatorTool + 动态工具
     */
    private List<ToolMetadata> buildAvailableTools() {
        List<ToolMetadata> tools = new ArrayList<>();

        // 1. 远程工具（已注册的 SSE 实例）
        for (ServiceInstance instance : instanceRegistry.getOnlineInstances()) {
            if (instance.getTools() == null) continue;
            for (ServiceInstance.ToolInfo info : instance.getTools()) {
                tools.add(new ToolMetadata(
                    info.getName(),
                    info.getDescription(),
                    info.getParameterSchema(),
                    instance.getAddress()
                ));
            }
        }

        // 2. 内建 ToolCreatorTool（始终注入）
        ToolCreatorTool.ToolMetadata creatorMeta = toolCreatorTool.getMetadata();
        tools.add(new ToolMetadata(
            creatorMeta.name,
            creatorMeta.description,
            creatorMeta.parameterSchema,
            "internal"
        ));

        // 3. 已注册的动态工具
        for (String toolName : dynamicToolRegistry.getAllToolNames()) {
            var dynamicTool = dynamicToolRegistry.getTool(toolName);
            tools.add(new ToolMetadata(
                toolName,
                dynamicTool.getDescription(),
                "{}",
                "dynamic"
            ));
        }

        log.info("[ReAct] available tools: {}", tools.stream().map(ToolMetadata::getName).toList());
        return tools;
    }

    // ========== <thinking> 解析 ==========

    private String extractThinking(String response) {
        Matcher m = THINKING_PATTERN.matcher(response);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }

    private String removeThinking(String response) {
        return THINKING_PATTERN.matcher(response).replaceAll("").trim();
    }

    // ========== LLM 响应解析 ==========

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

                // 只截取第一个 JSON 对象（防止 LLM 输出多余内容）
                jsonPart = extractFirstJson(jsonPart);

                JsonNode node = objectMapper.readTree(jsonPart);
                ToolCall toolCall = new ToolCall();
                toolCall.setName(node.get("name").asText());

                Map<String, Object> args = new LinkedHashMap<>();
                JsonNode argsNode = node.get("args");
                if (argsNode != null) {
                    argsNode.fields().forEachRemaining(entry -> {
                        JsonNode v = entry.getValue();
                        if (v.isNumber())      args.put(entry.getKey(), v.numberValue());
                        else if (v.isBoolean()) args.put(entry.getKey(), v.booleanValue());
                        else if (v.isNull())    args.put(entry.getKey(), null);
                        else                    args.put(entry.getKey(), v.asText());
                    });
                }
                toolCall.setArguments(args);
                parsed.setHasToolCall(true);
                parsed.setToolCall(toolCall);

            } catch (Exception e) {
                log.error("[ReAct] failed to parse tool call", e);
            }
        }

        return parsed;
    }

    /**
     * 从字符串中提取第一个完整 JSON 对象（防止 LLM 在 JSON 后附加说明文字）
     */
    private String extractFirstJson(String text) {
        int depth = 0;
        int start = text.indexOf('{');
        if (start == -1) return text;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return text.substring(start, i + 1);
            }
        }
        return text;
    }

    // ========== SSE 推流 ==========

    private void sendEvent(SseEmitter emitter, AgentStreamEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event().data(json).name("agent-event"));
            log.debug("[SSE] sent event type={}", event.type());
        } catch (IOException e) {
            log.error("[SSE] failed to send event", e);
            throw new RuntimeException("SSE send failed", e);
        }
    }

    // ========== 内部数据类 ==========

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
