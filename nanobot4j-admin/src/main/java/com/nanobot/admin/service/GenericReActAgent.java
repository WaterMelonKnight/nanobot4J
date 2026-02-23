package com.nanobot.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.admin.domain.ServiceInstance;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用 ReAct Agent 执行器 - 完全动态化，无硬编码
 *
 * 核心理念：
 * 1. 不依赖任何具体工具名称
 * 2. 动态构建 Prompt，包含所有在线工具
 * 3. 通用的 ReAct 循环：思考 -> 行动 -> 观察 -> 思考...
 * 4. 通过 JSON 解析 LLM 的工具调用意图
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenericReActAgent {

    private final InstanceRegistry instanceRegistry;
    private final RemoteToolExecutor remoteToolExecutor;
    private final LLMService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_ITERATIONS = 10; // 防止无限循环
    private static final String TOOL_CALL_MARKER = "TOOL_CALL:";
    private static final String FINAL_ANSWER_MARKER = "FINAL_ANSWER:";

    /**
     * 处理用户消息 - 通用 ReAct 循环
     */
    public AgentResponse chat(String userMessage) {
        log.info("Processing user message with generic ReAct agent: {}", userMessage);

        AgentResponse response = new AgentResponse();
        response.setUserMessage(userMessage);
        response.setTimestamp(System.currentTimeMillis());

        // 1. 获取所有在线工具
        List<ToolMetadata> availableTools = getAvailableTools();
        if (availableTools.isEmpty()) {
            response.setFinalAnswer("抱歉，当前没有可用的工具。");
            response.addThought("❌ 没有在线的工具实例");
            return response;
        }

        response.setAvailableTools(availableTools.stream()
            .map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("name", t.getName());
                map.put("description", t.getDescription());
                return map;
            })
            .collect(Collectors.toList()));

        // 2. 初始化对话历史
        List<String> conversationHistory = new ArrayList<>();
        conversationHistory.add("User: " + userMessage);

        // 3. ReAct 循环
        response.addThought("🤔 开始分析任务...");

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            log.info("ReAct iteration {}/{}", iteration + 1, MAX_ITERATIONS);

            // 3.1 构建动态 Prompt（包含对话历史）
            String systemPrompt = buildDynamicPrompt(availableTools, conversationHistory);

            // 3.2 调用真实 LLM（第一次传用户消息，后续传"继续"）
            String currentMessage = (iteration == 0) ? userMessage : "请根据上面的工具返回结果，继续分析并给出最终答案。";
            String llmResponse = llmService.chat(systemPrompt, currentMessage);

            log.info("LLM Response: {}", llmResponse);
            response.addThought("💭 " + llmResponse);

            // 3.3 解析 LLM 响应
            ParsedResponse parsed = parseLLMResponse(llmResponse);

            if (parsed.isFinalAnswer()) {
                // 找到最终答案，结束循环
                response.setFinalAnswer(parsed.getAnswer());
                response.addThought("✅ 任务完成");
                break;
            }

            if (parsed.isHasToolCall()) {
                // 需要调用工具
                ToolCall toolCall = parsed.getToolCall();
                response.addThought("🔧 调用工具: " + toolCall.getName());

                // 记录工具调用
                response.addToolCall(toolCall.getName(), toolCall.getArguments());

                // 执行远程工具调用
                String toolResult = executeToolCall(toolCall);

                response.addThought("📊 工具返回: " + toolResult);

                // 将观察结果加入对话历史
                conversationHistory.add("Tool Call: " + toolCall.getName() + " with args: " + toolCall.getArguments());
                conversationHistory.add("Observation: " + toolResult);

            } else {
                // LLM 没有明确指示，可能是思考过程
                conversationHistory.add("Agent: " + llmResponse);
            }
        }

        // 如果循环结束还没有答案
        if (response.getFinalAnswer() == null) {
            response.setFinalAnswer("抱歉，我无法完成这个任务。");
            response.addThought("⚠️ 达到最大迭代次数");
        }

        return response;
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
     * 动态构建 Prompt - 包含所有可用工具
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
     * 模拟 LLM 响应（实际应该调用真实 LLM API）
     * 这里用规则引擎模拟，展示如何动态处理
     */
    private String simulateLLMResponse(String userMessage, List<ToolMetadata> tools,
                                       List<String> history) {
        // 如果有观察结果，生成最终答案（优先检查）
        if (history.stream().anyMatch(h -> h.startsWith("Observation:"))) {
            String lastObservation = history.stream()
                .filter(h -> h.startsWith("Observation:"))
                .reduce((first, second) -> second)
                .orElse("");

            return "FINAL_ANSWER: " + lastObservation.replace("Observation: ", "");
        }

        String lowerMessage = userMessage.toLowerCase();

        // 检查是否需要调用工具（通用规则）
        for (ToolMetadata tool : tools) {
            String toolName = tool.getName().toLowerCase();
            String description = tool.getDescription().toLowerCase();

            // 改进的关键词匹配（支持中英文）
            boolean shouldCallTool = false;

            // 检查工具名匹配
            if (lowerMessage.contains(toolName)) {
                shouldCallTool = true;
            }

            // 检查描述中的关键词
            if (description.contains("天气") && lowerMessage.contains("天气")) {
                shouldCallTool = true;
            }
            if (description.contains("计算") && (lowerMessage.contains("计算") || lowerMessage.contains("加") ||
                lowerMessage.contains("减") || lowerMessage.contains("乘") || lowerMessage.contains("除"))) {
                shouldCallTool = true;
            }
            if (description.contains("时间") && (lowerMessage.contains("时间") || lowerMessage.contains("几点"))) {
                shouldCallTool = true;
            }

            if (shouldCallTool) {
                // 尝试提取参数
                Map<String, Object> args = extractArgumentsFromMessage(userMessage, tool);

                try {
                    String argsJson = objectMapper.writeValueAsString(args);
                    return String.format("TOOL_CALL: {\"name\": \"%s\", \"args\": %s}",
                                       tool.getName(), argsJson);
                } catch (Exception e) {
                    log.error("Failed to serialize args", e);
                }
            }
        }

        return "I need more information to help you.";
    }

    /**
     * 从用户消息中提取参数（通用方法）
     */
    private Map<String, Object> extractArgumentsFromMessage(String message, ToolMetadata tool) {
        Map<String, Object> args = new HashMap<>();

        // 这里是简化的参数提取逻辑
        // 实际应该解析 parameterSchema 并智能提取

        try {
            JsonNode schema = objectMapper.readTree(tool.getParameterSchema());
            JsonNode properties = schema.get("properties");

            if (properties != null) {
                properties.fields().forEachRemaining(entry -> {
                    String paramName = entry.getKey();
                    // 简单的关键词提取（实际 LLM 会更准确）
                    args.put(paramName, extractParamValue(message, paramName));
                });
            }
        } catch (Exception e) {
            log.warn("Failed to parse parameter schema", e);
        }

        return args;
    }

    /**
     * 提取参数值（简化版）
     */
    private Object extractParamValue(String message, String paramName) {
        // 这里是占位实现，实际应该用 NLP 或 LLM 提取
        if (paramName.equals("city")) {
            // 提取城市名 - 改进版，支持常见城市名
            String[] commonCities = {"北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "武汉", "西安", "重庆"};
            for (String city : commonCities) {
                if (message.contains(city)) {
                    return city;
                }
            }
            // 如果没有匹配到常见城市，尝试提取第一个中文词
            String[] words = message.split("[，。？！、\\s]+");
            for (String word : words) {
                if (word.matches("[\u4e00-\u9fa5]{2,}")) {
                    return word;
                }
            }
        }

        // 计算器参数提取
        if (paramName.equals("operation")) {
            if (message.contains("加") || message.contains("+")) return "add";
            if (message.contains("减") || message.contains("-")) return "subtract";
            if (message.contains("乘") || message.contains("×") || message.contains("*")) return "multiply";
            if (message.contains("除") || message.contains("÷") || message.contains("/")) return "divide";
        }

        if (paramName.equals("a") || paramName.equals("b")) {
            // 提取数字
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
            java.util.regex.Matcher matcher = pattern.matcher(message);
            java.util.List<String> numbers = new java.util.ArrayList<>();
            while (matcher.find()) {
                numbers.add(matcher.group());
            }
            if (paramName.equals("a") && numbers.size() > 0) {
                return Double.parseDouble(numbers.get(0));
            }
            if (paramName.equals("b") && numbers.size() > 1) {
                return Double.parseDouble(numbers.get(1));
            }
        }

        return "";
    }

    /**
     * 解析 LLM 响应
     */
    private ParsedResponse parseLLMResponse(String response) {
        ParsedResponse parsed = new ParsedResponse();

        if (response.contains(FINAL_ANSWER_MARKER)) {
            // 找到最终答案
            String answer = response.substring(response.indexOf(FINAL_ANSWER_MARKER) + FINAL_ANSWER_MARKER.length()).trim();
            parsed.setFinalAnswer(true);
            parsed.setAnswer(answer);
            return parsed;
        }

        if (response.contains(TOOL_CALL_MARKER)) {
            // 找到工具调用
            try {
                String jsonPart = response.substring(response.indexOf(TOOL_CALL_MARKER) + TOOL_CALL_MARKER.length()).trim();
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

    /**
     * 工具元数据
     */
    @Data
    private static class ToolMetadata {
        private final String name;
        private final String description;
        private final String parameterSchema;
        private final String instanceAddress;
    }

    /**
     * 工具调用
     */
    @Data
    private static class ToolCall {
        private String name;
        private Map<String, Object> arguments;
    }

    /**
     * 解析后的响应
     */
    @Data
    private static class ParsedResponse {
        private boolean finalAnswer;
        private String answer;
        private boolean hasToolCall;
        private ToolCall toolCall;
    }

    /**
     * Agent 响应
     */
    @Data
    public static class AgentResponse {
        private String userMessage;
        private List<String> thoughts = new ArrayList<>();
        private List<Map<String, Object>> toolCalls = new ArrayList<>();
        private String finalAnswer;
        private long timestamp;
        private List<Map<String, Object>> availableTools = new ArrayList<>();

        public void addThought(String thought) {
            this.thoughts.add(thought);
        }

        public void addToolCall(String toolName, Map<String, Object> params) {
            Map<String, Object> call = new HashMap<>();
            call.put("tool", toolName);
            call.put("params", params);
            call.put("timestamp", System.currentTimeMillis());
            this.toolCalls.add(call);
        }
    }
}
