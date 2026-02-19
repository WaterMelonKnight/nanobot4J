package com.nanobot.llm.annotation;

import java.lang.annotation.*;

/**
 * 指定使用特定的 LLM 模型
 *
 * 使用示例：
 * <pre>
 * {@code
 * @Service
 * public class MyService {
 *
 *     @UseModel("deepseek")
 *     public String generateCode(String prompt) {
 *         // 这个方法会使用 DeepSeek 模型
 *     }
 *
 *     @UseModel("kimi")
 *     public String analyzeText(String text) {
 *         // 这个方法会使用 Kimi 模型
 *     }
 * }
 * }
 * </pre>
 *
 * 注意：
 * 1. 如果不指定 @UseModel，则使用默认模型
 * 2. 模型名称必须在配置文件中存在且已启用
 * 3. 该注解通过 AOP 实现，需要方法是 public 的
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UseModel {

    /**
     * 模型名称（如 "deepseek", "kimi", "ollama"）
     */
    String value();

    /**
     * 是否启用降级策略
     * 如果为 true，当指定模型失败时会自动降级到其他模型
     * 如果为 false，当指定模型失败时会直接抛出异常
     */
    boolean enableFallback() default true;
}
