package com.nanobot.starter.autoconfigure;

import com.nanobot.starter.registry.AdminReporter;
import com.nanobot.starter.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nanobot 自动配置类
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(NanobotProperties.class)
public class NanobotAutoConfiguration {

    @Bean
    public ToolRegistry toolRegistry() {
        log.info("Initializing ToolRegistry");
        return new ToolRegistry();
    }

    @Bean
    public ToolScanner toolScanner(ToolRegistry toolRegistry) {
        log.info("Initializing ToolScanner");
        return new ToolScanner(toolRegistry);
    }

    @Bean
    @ConditionalOnProperty(prefix = "nanobot.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AdminReporter adminReporter(NanobotProperties properties, ToolRegistry toolRegistry) {
        log.info("Initializing AdminReporter with address: {}", properties.getAdmin().getAddress());
        return new AdminReporter(properties, toolRegistry);
    }
}
