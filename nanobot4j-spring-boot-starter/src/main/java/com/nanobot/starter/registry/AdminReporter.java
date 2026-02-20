package com.nanobot.starter.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.core.tool.ToolDefinition;
import com.nanobot.starter.autoconfigure.NanobotProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Admin 上报器 - 负责向 Admin 服务注册和发送心跳
 */
@Slf4j
public class AdminReporter {

    private final NanobotProperties properties;
    private final ToolRegistry toolRegistry;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;

    private String instanceId;
    private String instanceAddress;

    public AdminReporter(NanobotProperties properties, ToolRegistry toolRegistry) {
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!properties.getAdmin().isEnabled()) {
            log.info("Admin registration is disabled");
            return;
        }

        try {
            // 获取本机信息
            initInstanceInfo(event);

            // 注册到 Admin
            register();

            // 启动心跳线程
            startHeartbeat();

        } catch (Exception e) {
            log.error("Failed to initialize AdminReporter", e);
        }
    }

    private void initInstanceInfo(ApplicationReadyEvent event) throws Exception {
        // 生成实例 ID
        this.instanceId = InetAddress.getLocalHost().getHostName() + "-" + System.currentTimeMillis();

        // 获取应用端口
        String port = event.getApplicationContext().getEnvironment().getProperty("server.port", "8080");
        String host = InetAddress.getLocalHost().getHostAddress();
        this.instanceAddress = "http://" + host + ":" + port;

        log.info("Instance initialized: id={}, address={}", instanceId, instanceAddress);
    }

    private void register() {
        try {
            RegistrationRequest request = buildRegistrationRequest();
            String json = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(properties.getAdmin().getAddress() + "/api/registry/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Successfully registered to Admin: {}", properties.getAdmin().getAddress());
            } else {
                log.error("Failed to register to Admin, status: {}, body: {}", 
                    response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to register to Admin", e);
        }
    }

    private void startHeartbeat() {
        int interval = properties.getAdmin().getHeartbeatInterval();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeat();
            } catch (Exception e) {
                log.error("Failed to send heartbeat", e);
            }
        }, interval, interval, TimeUnit.SECONDS);

        log.info("Heartbeat started with interval: {} seconds", interval);
    }

    private void sendHeartbeat() {
        try {
            HeartbeatRequest request = new HeartbeatRequest(instanceId, System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(properties.getAdmin().getAddress() + "/api/registry/beat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.debug("Heartbeat failed: {}", e.getMessage());
        }
    }

    private RegistrationRequest buildRegistrationRequest() {
        RegistrationRequest request = new RegistrationRequest();
        request.setInstanceId(instanceId);
        request.setAddress(instanceAddress);
        request.setTimestamp(System.currentTimeMillis());

        // 收集所有工具信息
        List<ToolInfo> toolInfos = new ArrayList<>();
        for (Map.Entry<String, ToolDefinition> entry : toolRegistry.getAllTools().entrySet()) {
            ToolDefinition def = entry.getValue();
            ToolInfo info = new ToolInfo();
            info.setName(def.getName());
            info.setDescription(def.getDescription());
            info.setParameterSchema(def.getParameterSchema());
            toolInfos.add(info);
        }
        request.setTools(toolInfos);

        return request;
    }

    @Data
    public static class RegistrationRequest {
        private String instanceId;
        private String address;
        private List<ToolInfo> tools;
        private long timestamp;
    }

    @Data
    public static class ToolInfo {
        private String name;
        private String description;
        private String parameterSchema;
    }

    @Data
    public static class HeartbeatRequest {
        private String instanceId;
        private long timestamp;

        public HeartbeatRequest(String instanceId, long timestamp) {
            this.instanceId = instanceId;
            this.timestamp = timestamp;
        }
    }
}
