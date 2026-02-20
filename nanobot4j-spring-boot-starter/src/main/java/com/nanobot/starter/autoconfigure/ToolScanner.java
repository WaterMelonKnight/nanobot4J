package com.nanobot.starter.autoconfigure;

import com.nanobot.starter.annotation.NanobotTool;
import com.nanobot.starter.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * Tool 扫描器 - 扫描所有 Spring Bean 中的 @NanobotTool 注解
 */
@Slf4j
public class ToolScanner implements BeanPostProcessor {

    private final ToolRegistry toolRegistry;

    public ToolScanner(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        // 扫描所有方法
        for (Method method : targetClass.getDeclaredMethods()) {
            NanobotTool annotation = AnnotationUtils.findAnnotation(method, NanobotTool.class);
            if (annotation != null) {
                // 注册工具
                toolRegistry.registerMethodTool(
                    annotation.name(),
                    annotation.description(),
                    annotation.parameterSchema(),
                    bean,
                    method
                );
                log.info("Discovered @NanobotTool: {} in bean: {}", annotation.name(), beanName);
            }
        }

        return bean;
    }
}
