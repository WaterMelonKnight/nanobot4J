package com.nanobot.tool;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表 - 管理所有可用的工具
 *
 * 设计思路：
 * 1. 使用 Spring 的依赖注入自动注册所有 Tool 实现
 * 2. 线程安全的工具查找
 * 3. 支持动态添加/移除工具
 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    /**
     * 构造函数 - Spring 会自动注入所有 Tool 实现
     */
    public ToolRegistry(List<Tool> toolList) {
        toolList.forEach(this::registerTool);
    }

    /**
     * 注册工具
     */
    public void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * 移除工具
     */
    public void unregisterTool(String toolName) {
        tools.remove(toolName);
    }

    /**
     * 根据名称获取工具
     */
    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * 获取所有工具
     */
    public Collection<Tool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}
