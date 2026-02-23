package com.nanobot.admin.service;

import com.nanobot.admin.domain.ServiceInstance;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 增强版 Agent 服务 - 支持多步骤推理和思考过程展示
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedAgentService {

    private final InstanceRegistry instanceRegistry;
    private final RemoteToolExecutor remoteToolExecutor;

    /**
     * 处理用户消息，支持多步骤工具调用
     */
    public AgentResponse chat(String userMessage) {
        log.info("Processing user message: {}", userMessage);
        
        AgentResponse response = new AgentResponse();
        response.setUserMessage(userMessage);
        response.setTimestamp(System.currentTimeMillis());

        // 1. 获取所有在线实例的工具
        List<ServiceInstance> onlineInstances = instanceRegistry.getOnlineInstances();
        if (onlineInstances.isEmpty()) {
            response.setFinalAnswer("抱歉，当前没有可用的服务实例。");
            return response;
        }

        // 2. 收集所有可用工具
        List<ToolInfo> availableTools = collectTools(onlineInstances);
        response.setAvailableTools(availableTools.stream()
            .map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("name", t.name);
                map.put("description", t.description);
                return map;
            })
            .collect(Collectors.toList()));

        // 3. 分析任务并规划步骤
        response.addThought("🤔 分析任务：" + userMessage);
        TaskPlan plan = analyzeAndPlan(userMessage, availableTools);
        
        if (plan.steps.isEmpty()) {
            response.addThought("❌ 无法理解该任务或找不到合适的工具");
            response.setFinalAnswer(
                "我理解了你的问题，但目前没有找到合适的工具来处理。\n\n可用工具：\n" +
                availableTools.stream()
                    .map(t -> "- " + t.name + ": " + t.description)
                    .collect(Collectors.joining("\n"))
            );
            return response;
        }

        // 4. 显示执行计划
        response.addThought("📋 执行计划：");
        for (int i = 0; i < plan.steps.size(); i++) {
            response.addThought("  步骤 " + (i + 1) + ": " + plan.steps.get(i).description);
        }

        // 5. 执行每个步骤
        Map<String, Object> context = new HashMap<>();
        for (int i = 0; i < plan.steps.size(); i++) {
            TaskStep step = plan.steps.get(i);
            response.addThought("\n⚙️ 执行步骤 " + (i + 1) + ": " + step.description);
            
            String result = executeStep(step, context, response);
            context.put("step_" + i + "_result", result);
            
            response.addThought("✅ 结果: " + result);
        }

        // 6. 生成最终答案
        String finalAnswer = generateFinalAnswer(plan, context);
        response.setFinalAnswer(finalAnswer);
        response.addThought("\n💡 最终答案: " + finalAnswer);

        return response;
    }

    /**
     * 收集所有可用工具
     */
    private List<ToolInfo> collectTools(List<ServiceInstance> instances) {
        List<ToolInfo> tools = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            if (instance.getTools() != null) {
                for (ServiceInstance.ToolInfo tool : instance.getTools()) {
                    tools.add(new ToolInfo(
                        tool.getName(),
                        tool.getDescription(),
                        instance.getInstanceId(),
                        instance.getAddress()
                    ));
                }
            }
        }
        return tools;
    }

    /**
     * 分析任务并生成执行计划
     */
    private TaskPlan analyzeAndPlan(String message, List<ToolInfo> tools) {
        TaskPlan plan = new TaskPlan();
        String lowerMessage = message.toLowerCase();

        // 检测是否需要多步骤执行
        boolean needsWeather = lowerMessage.contains("天气") || lowerMessage.contains("气温");
        boolean needsCalculation = containsCalculation(lowerMessage);
        boolean needsTime = lowerMessage.contains("时间") || lowerMessage.contains("几点");

        // 场景1: 多城市天气 + 计算
        if (needsWeather && needsCalculation) {
            List<String> cities = extractCities(message);
            if (cities.size() >= 2) {
                for (String city : cities) {
                    plan.addStep(new TaskStep(
                        "weather",
                        "查询" + city + "的天气",
                        Map.of("city", city),
                        findTool("weather", tools)
                    ));
                }
                plan.addStep(new TaskStep(
                    "calculator",
                    "计算气温总和",
                    Map.of("operation", "add"),
                    findTool("calculator", tools)
                ));
                plan.setNeedsAggregation(true);
                return plan;
            }
        }

        // 场景2: 单个工具调用
        if (needsCalculation) {
            Map<String, Object> params = extractCalculationParams(message);
            plan.addStep(new TaskStep(
                "calculator",
                "执行数学计算",
                params,
                findTool("calculator", tools)
            ));
        } else if (needsWeather) {
            String city = extractCities(message).stream().findFirst().orElse("北京");
            plan.addStep(new TaskStep(
                "weather",
                "查询" + city + "天气",
                Map.of("city", city),
                findTool("weather", tools)
            ));
        } else if (needsTime) {
            plan.addStep(new TaskStep(
                "time",
                "获取当前时间",
                Map.of(),
                findTool("time", tools)
            ));
        }

        return plan;
    }

    /**
     * 执行单个步骤
     */
    private String executeStep(TaskStep step, Map<String, Object> context, AgentResponse response) {
        if (step.tool == null) {
            return "工具不可用";
        }

        response.addToolCall(step.toolName, step.params);

        // 如果参数依赖前面步骤的结果，需要填充
        Map<String, Object> actualParams = new HashMap<>(step.params);
        if (step.toolName.equals("calculator") && context.size() > 0) {
            // 从上下文中提取数字
            List<Double> numbers = new ArrayList<>();
            for (Object value : context.values()) {
                Double num = extractNumberFromResult(value.toString());
                if (num != null) {
                    numbers.add(num);
                }
            }
            if (numbers.size() >= 2) {
                actualParams.put("a", numbers.get(0));
                actualParams.put("b", numbers.get(1));
            }
        }

        return invokeTool(step.tool, actualParams);
    }

    /**
     * 生成最终答案
     */
    private String generateFinalAnswer(TaskPlan plan, Map<String, Object> context) {
        if (context.isEmpty()) {
            return "任务执行失败";
        }

        if (plan.isNeedsAggregation()) {
            // 多步骤任务，需要汇总结果
            StringBuilder answer = new StringBuilder();
            for (int i = 0; i < context.size(); i++) {
                Object result = context.get("step_" + i + "_result");
                if (result != null) {
                    answer.append(result.toString()).append("\n");
                }
            }
            return answer.toString().trim();
        } else {
            // 单步骤任务，直接返回结果
            return context.get("step_0_result").toString();
        }
    }

    /**
     * 调用工具 - 通过远程 RPC 调用
     */
    private String invokeTool(ToolInfo tool, Map<String, Object> params) {
        log.info("Invoking remote tool: {} with params: {}", tool.name, params);
        return remoteToolExecutor.executeRemoteTool(tool.name, params);
    }

    // ========== 辅助方法 ==========

    private boolean containsCalculation(String message) {
        return message.matches(".*[0-9]+.*[+\\-*/×÷].*[0-9]+.*") ||
               message.contains("计算") || message.contains("相加") ||
               message.contains("相减") || message.contains("相乘") ||
               message.contains("相除") || message.contains("总和");
    }

    private List<String> extractCities(String message) {
        List<String> cities = new ArrayList<>();
        String[] cityNames = {"北京", "上海", "广州", "深圳", "杭州", "成都", "西安", "武汉", "南京", "重庆"};
        for (String city : cityNames) {
            if (message.contains(city)) {
                cities.add(city);
            }
        }
        return cities;
    }

    private Map<String, Object> extractCalculationParams(String message) {
        Map<String, Object> params = new HashMap<>();

        if (message.contains("+") || message.contains("加") || message.contains("相加")) {
            params.put("operation", "add");
        } else if (message.contains("-") || message.contains("减")) {
            params.put("operation", "subtract");
        } else if (message.contains("*") || message.contains("×") || message.contains("乘")) {
            params.put("operation", "multiply");
        } else if (message.contains("/") || message.contains("÷") || message.contains("除")) {
            params.put("operation", "divide");
        }

        Pattern pattern = Pattern.compile("\\d+\\.?\\d*");
        Matcher matcher = pattern.matcher(message);
        List<Double> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(Double.parseDouble(matcher.group()));
        }

        if (numbers.size() >= 2) {
            params.put("a", numbers.get(0));
            params.put("b", numbers.get(1));
        }

        return params;
    }

    private Double extractNumberFromResult(String result) {
        Pattern pattern = Pattern.compile("\\d+\\.?\\d*");
        Matcher matcher = pattern.matcher(result);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group());
        }
        return null;
    }

    private double convertToDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return Double.parseDouble(obj.toString());
    }

    private ToolInfo findTool(String name, List<ToolInfo> tools) {
        return tools.stream()
            .filter(t -> t.name.equals(name))
            .findFirst()
            .orElse(null);
    }

    // ========== 数据类 ==========

    @Data
    public static class AgentResponse {
        private String userMessage;
        private List<String> thoughts = new ArrayList<>();
        private List<Map<String, Object>> toolCalls = new ArrayList<>();
        private String finalAnswer;
        private long timestamp;
        private List<Map<String, Object>> availableTools;

        public void addThought(String thought) {
            this.thoughts.add(thought);
        }

        public void addToolCall(String toolName, Map<String, Object> params) {
            this.toolCalls.add(Map.of(
                "tool", toolName,
                "params", params,
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    private static class TaskPlan {
        List<TaskStep> steps = new ArrayList<>();
        boolean needsAggregation = false;

        void addStep(TaskStep step) {
            steps.add(step);
        }

        boolean isNeedsAggregation() {
            return needsAggregation;
        }

        void setNeedsAggregation(boolean needsAggregation) {
            this.needsAggregation = needsAggregation;
        }
    }

    private static class TaskStep {
        String toolName;
        String description;
        Map<String, Object> params;
        ToolInfo tool;

        TaskStep(String toolName, String description, Map<String, Object> params, ToolInfo tool) {
            this.toolName = toolName;
            this.description = description;
            this.params = params;
            this.tool = tool;
        }
    }

    private record ToolInfo(String name, String description, String instanceId, String address) {}
}
