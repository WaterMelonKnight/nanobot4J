package com.nanobot.llm.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @UseModel 注解的 AOP 切面
 *
 * 职责：
 * 1. 拦截带有 @UseModel 注解的方法
 * 2. 将模型名称存储到 ThreadLocal 中
 * 3. 方法执行完毕后清理 ThreadLocal
 *
 * 设计思路：
 * - 使用 ThreadLocal 存储当前线程的模型选择
 * - LLMService 在调用时会检查 ThreadLocal 中的模型名称
 * - 这样可以在不修改方法签名的情况下实现模型选择
 */
@Aspect
@Component
public class ModelSelectionAspect {

    private static final Logger log = LoggerFactory.getLogger(ModelSelectionAspect.class);

    /**
     * ThreadLocal 存储当前线程选择的模型
     */
    private static final ThreadLocal<ModelContext> MODEL_CONTEXT = new ThreadLocal<>();

    /**
     * 拦截带有 @UseModel 注解的方法
     */
    @Around("@annotation(com.nanobot.llm.annotation.UseModel)")
    public Object aroundUseModel(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        UseModel useModel = method.getAnnotation(UseModel.class);

        if (useModel == null) {
            return joinPoint.proceed();
        }

        String modelName = useModel.value();
        boolean enableFallback = useModel.enableFallback();

        log.debug("Method {} is annotated with @UseModel(\"{}\")", method.getName(), modelName);

        // 设置模型上下文
        ModelContext context = new ModelContext(modelName, enableFallback);
        MODEL_CONTEXT.set(context);

        try {
            // 执行目标方法
            return joinPoint.proceed();
        } finally {
            // 清理 ThreadLocal
            MODEL_CONTEXT.remove();
        }
    }

    /**
     * 获取当前线程的模型上下文
     */
    public static ModelContext getCurrentModelContext() {
        return MODEL_CONTEXT.get();
    }

    /**
     * 检查当前线程是否有模型上下文
     */
    public static boolean hasModelContext() {
        return MODEL_CONTEXT.get() != null;
    }

    /**
     * 模型上下文
     */
    public static class ModelContext {
        private final String modelName;
        private final boolean enableFallback;

        public ModelContext(String modelName, boolean enableFallback) {
            this.modelName = modelName;
            this.enableFallback = enableFallback;
        }

        public String getModelName() {
            return modelName;
        }

        public boolean isEnableFallback() {
            return enableFallback;
        }
    }
}
