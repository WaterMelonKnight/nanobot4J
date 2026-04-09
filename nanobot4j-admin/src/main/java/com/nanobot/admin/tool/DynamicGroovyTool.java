package com.nanobot.admin.tool;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 动态 Groovy 工具 - 运行时执行 Groovy 脚本
 *
 * 核心特性：
 * 1. 接收 Groovy 脚本代码，动态执行
 * 2. 支持参数传递（通过 Binding）
 * 3. 超时控制（5 秒）
 * 4. 异常隔离（不会导致主线程崩溃）
 *
 * 安全机制：
 * - 使用 ExecutorService 执行，支持超时中断
 * - 捕获所有异常并转换为文本返回
 * - 限制执行时间，防止死循环
 */
@Slf4j
public class DynamicGroovyTool {

    private final String toolName;
    private final String description;
    private final String groovyScript;

    /**
     * 脚本执行超时时间（秒）
     */
    private static final int TIMEOUT_SECONDS = 5;

    /**
     * 线程池（用于超时控制）
     */
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("groovy-executor-" + System.currentTimeMillis());
        return thread;
    });

    public DynamicGroovyTool(String toolName, String description, String groovyScript) {
        this.toolName = toolName;
        this.description = description;
        this.groovyScript = groovyScript;
    }

    /**
     * 执行工具
     *
     * @param parameters 参数 Map
     * @return 执行结果（字符串形式）
     */
    public String execute(Map<String, Object> parameters) {
        log.info("Executing dynamic Groovy tool: {}", toolName);
        log.debug("Script: ", groovyScript);
        log.debug("Parameters: {}", parameters);

        try {
            // 使用 Future 实现超时控制
            Future<String> future = executor.submit(() -> executeScript(parameters));

            // 等待结果，最多 TIMEOUT_SECONDS 秒
            String result = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Tool {} executed successfully", toolName);
            return result;

        } catch (TimeoutException e) {
            log.error("Tool {} execution timeout after {} seconds", toolName, TIMEOUT_SECONDS);
            return "Error: Script execution timeout after " + TIMEOUT_SECONDS + " seconds";

        } catch (InterruptedException e) {
            log.error("Tool {} execution interrupted", toolName);
            Thread.currentThread().interrupt();
            return "Error: Script execution interrupted";

        } catch (ExecutionException e) {
            log.error("Tool {} execution failed", toolName, e);
            Throwable cause = e.getCause();
            return "Error: " + (cause != null ? cause.getMessage() : e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error executing tool {}", toolName, e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 实际执行 Groovy 脚本（在独立线程中）
     */
    private String executeScript(Map<String, Object> parameters) {
        try {
            // 1. 创建 Binding，注入参数
            Binding binding = new Binding();

            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    binding.setVariable(entry.getKey(), entry.getValue());
                }
            }

            // 2. 创建 GroovyShell
            GroovyShell shell = new GroovyShell(binding);

            // 3. 执行脚本
            Object result = shell.evaluate(groovyScript);

            // 4. 转换结果为字符串
            if (result == null) {
                return "null";
            }

            return result.toString();

        } catch (Exception e) {
            log.error("Groovy script execution error", e);
            throw new RuntimeException("Script execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取工具名称
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * 获取工具描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取脚本内容（用于调试）
     */
    public String getGroovyScript() {
        return groovyScript;
    }
}
