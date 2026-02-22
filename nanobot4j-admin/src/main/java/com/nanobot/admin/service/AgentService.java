package com.nanobot.admin.service;

import com.nanobot.admin.domain.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 服务 - 处理对话和工具调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final InstanceRegistry instanceRegistry;

    /**
     * 处理用户消息，选择并调用合适的工具
     */
    public Map<String, Object> chat(String userMessage) {
        log.info("Processing user message: {}", userMessage);

        // 1. 获取所有在线实例的工具
        List<ServiceInstance> onlineInstances = instanceRegistry.getOnlineInstances();
        if (onlineInstances.isEmpty()) {
            return createResponse("抱歉，当前没有可用的服务实例。", null, null);
        }

        // 2. 收集所有可用工具
        List<ToolInfo> availableTools = new ArrayList<>();
        for (ServiceInstance instance : onlineInstances) {
            if (instance.getTools() != null) {
                for (ServiceInstance.ToolInfo tool : instance.getTools()) {
                    availableTools.add(new ToolInfo(
                        tool.getName(),
                        tool.getDescription(),
                        instance.getInstanceId(),
                        instance.getAddress()
                    ));
                }
            }
        }

        // 3. 简单的工具选择逻辑（基于关键词匹配）
        ToolInfo selectedTool = selectTool(userMessage, availableTools);
        if (selectedTool == null) {
            return createResponse(
                "我理解了你的问题，但目前没有找到合适的工具来处理。\n\n可用工具：\n" +
                availableTools.stream()
                    .map(t -> "- " + t.name + ": " + t.description)
                    .collect(Collectors.joining("\n")),
                null,
                availableTools
            );
        }

        // 4. 提取参数并调用工具
        Map<String, Object> params = extractParameters(userMessage, selectedTool.name);
        String result = invokeTool(selectedTool, params);

        return createResponse(result, selectedTool.name, availableTools);
    }

    /**
     * 选择合适的工具
     */
    private ToolInfo selectTool(String message, List<ToolInfo> tools) {
        String lowerMessage = message.toLowerCase();

        // 关键词匹配规则
        for (ToolInfo tool : tools) {
            if (tool.name.equals("calculator")) {
                if (lowerMessage.matches(".*[0-9]+.*[+\\-*/×÷].*[0-9]+.*") ||
                    lowerMessage.contains("计算") || lowerMessage.contains("加") ||
                    lowerMessage.contains("减") || lowerMessage.contains("乘") ||
                    lowerMessage.contains("除")) {
                    return tool;
                }
            } else if (tool.name.equals("weather")) {
                if (lowerMessage.contains("天气") || lowerMessage.contains("weather")) {
                    return tool;
                }
            } else if (tool.name.equals("time")) {
                if (lowerMessage.contains("时间") || lowerMessage.contains("几点") ||
                    lowerMessage.contains("time") || lowerMessage.contains("现在")) {
                    return tool;
                }
            }
        }

        return null;
    }

    /**
     * 从用户消息中提取参数
     */
    private Map<String, Object> extractParameters(String message, String toolName) {
        Map<String, Object> params = new HashMap<>();

        if ("calculator".equals(toolName)) {
            // 提取数学表达式
            if (message.contains("+") || message.contains("加")) {
                params.put("operation", "add");
            } else if (message.contains("-") || message.contains("减")) {
                params.put("operation", "subtract");
            } else if (message.contains("*") || message.contains("×") || message.contains("乘")) {
                params.put("operation", "multiply");
            } else if (message.contains("/") || message.contains("÷") || message.contains("除")) {
                params.put("operation", "divide");
            }

            // 提取数字
            String[] numbers = message.replaceAll("[^0-9.\\s]", " ").trim().split("\\s+");
            if (numbers.length >= 2) {
                try {
                    params.put("a", Double.parseDouble(numbers[0]));
                    params.put("b", Double.parseDouble(numbers[1]));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse numbers from message: {}", message);
                }
            }
        } else if ("weather".equals(toolName)) {
            // 提取城市名
            String city = message.replaceAll(".*(北京|上海|广州|深圳|杭州|成都|西安|武汉|南京|重庆).*", "$1");
            if (!city.equals(message)) {
                params.put("city", city);
            } else {
                params.put("city", "北京"); // 默认城市
            }
        }

        return params;
    }

    /**
     * 调用工具（模拟）
     */
    private String invokeTool(ToolInfo tool, Map<String, Object> params) {
        log.info("Invoking tool: {} with params: {}", tool.name, params);

        // 这里是模拟调用，实际应该通过 HTTP 调用远程服务
        if ("calculator".equals(tool.name)) {
            String op = (String) params.get("operation");
            Double a = (Double) params.get("a");
            Double b = (Double) params.get("b");

            if (op == null || a == null || b == null) {
                return "参数不完整，无法计算。请提供完整的数学表达式，例如：10 + 5";
            }

            double result = switch (op) {
                case "add" -> a + b;
                case "subtract" -> a - b;
                case "multiply" -> a * b;
                case "divide" -> b != 0 ? a / b : Double.NaN;
                default -> 0;
            };

            if (Double.isNaN(result)) {
                return "错误：除数不能为零";
            }

            return String.format("计算结果：%.2f %s %.2f = %.2f",
                a, getOperatorSymbol(op), b, result);
        } else if ("weather".equals(tool.name)) {
            String city = (String) params.getOrDefault("city", "北京");
            return String.format("%s今天天气：晴，温度 22°C，空气质量良好 ☀️", city);
        } else if ("time".equals(tool.name)) {
            return "当前时间：" + new Date();
        }

        return "工具调用失败";
    }

    private String getOperatorSymbol(String op) {
        return switch (op) {
            case "add" -> "+";
            case "subtract" -> "-";
            case "multiply" -> "×";
            case "divide" -> "÷";
            default -> "?";
        };
    }

    private Map<String, Object> createResponse(String message, String toolUsed,
                                                 List<ToolInfo> availableTools) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("toolUsed", toolUsed);
        response.put("timestamp", System.currentTimeMillis());
        if (availableTools != null) {
            response.put("availableTools", availableTools.stream()
                .map(t -> Map.of("name", t.name, "description", t.description))
                .collect(Collectors.toList()));
        }
        return response;
    }

    private record ToolInfo(String name, String description, String instanceId, String address) {}
}
