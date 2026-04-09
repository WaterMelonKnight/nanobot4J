package com.nanobot.admin.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态工具注册表
 *
 * 职责：
 * 1. 管理运行时创建的动态工具
 * 2. 提供工具注册、查询、执行接口
 * 3. 线程安全
 */
@Slf4j
@Component
public class DynamicToolRegistry {

    /**
     * 工具存储（线程安全）
     * Key: 工具名称
     * Value: DynamicGroovyTool 实例
     */
    private final Map<String, DynamicGroovyTool> tools = new ConcurrentHashMap<>();

    /**
     * 注册一个动态工具
     *
     * @param tool 工具实例
     */
    public void registerTool(DynamicGroovyTool tool) {
        String toolName = tool.getToolName();
        tools.put(toolName, tool);
        log.info("Registered dynamic tool: {} - {}", toolName, tool.getDescription());
    }

    /**
     * 获取工具
     *
     * @param toolName 工具名称
     * @return 工具实例，如果不存在返回 null
     */
    public DynamicGroovyTool getTool(String toolName) {
        return tools.get(toolName);
    }

    /**
     * 检查工具是否存在
     *
     * @param toolName 工具名称
     * @return 是否存在
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * 执行动态工具
     *
     * @param toolName 工具名称
     * @param parameters 参数
     * @return 执行结果
     */
    public String executeTool(String toolName, Map<String, Object> parameters) {
        DynamicGroovyTool tool = tools.get(toolName);

        if (tool == null) {
            log.warn("Tool not found: {}", toolName);
            return "Error: Tool '" + toolName + "' not found";
        }

        return tool.execute(parameters);
    }

    /**
     * 获取所有已注册的工具名称
     *
     * @return 工具名称列表
     */
    public java.util.Set<String> getAllToolNames() {
        return tools.keySet();
    }

    /**
     * 获取工具数量
     *
     * @return 工具数量
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * 移除工具
     *
     * @param toolName 工具名称
     */
    public void removeTool(String toolName) {
        DynamicGroovyTool removed = tools.remove(toolName);
        if (removed != null) {
            log.info("Removed dynamic tool: {}", toolName);
        }
    }

    /**
     * 清空所有工具
     */
    public void clearAll() {
        int count = tools.size();
        tools.clear();
        log.info("Cleared all {} dynamic tools", count);
    }
}
