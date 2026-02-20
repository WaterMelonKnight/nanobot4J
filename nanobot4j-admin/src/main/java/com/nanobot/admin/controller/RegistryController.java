package com.nanobot.admin.controller;

import com.nanobot.admin.domain.ServiceInstance;
import com.nanobot.admin.service.InstanceRegistry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 注册中心控制器 - 处理服务实例的注册和心跳
 */
@Slf4j
@RestController
@RequestMapping("/api/registry")
@RequiredArgsConstructor
public class RegistryController {

    private final InstanceRegistry instanceRegistry;

    /**
     * 服务注册接口
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegistrationRequest request) {
        log.info("Received registration request: {}", request);

        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceId(request.getInstanceId());
        instance.setServiceName(request.getServiceName());
        instance.setAddress(request.getAddress());
        instance.setTools(request.getTools());

        instanceRegistry.register(instance);

        return Map.of(
            "success", true,
            "message", "Registration successful",
            "instanceId", request.getInstanceId()
        );
    }

    /**
     * 心跳接口
     */
    @PostMapping("/beat")
    public Map<String, Object> heartbeat(@RequestBody HeartbeatRequest request) {
        log.debug("Received heartbeat from: {}", request.getInstanceId());

        instanceRegistry.heartbeat(request.getInstanceId());

        return Map.of(
            "success", true,
            "message", "Heartbeat received",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 获取所有实例
     */
    @GetMapping("/instances")
    public Map<String, Object> getAllInstances() {
        List<ServiceInstance> instances = instanceRegistry.getAllInstances();
        return Map.of(
            "success", true,
            "data", instances
        );
    }

    /**
     * 获取在线实例
     */
    @GetMapping("/instances/online")
    public Map<String, Object> getOnlineInstances() {
        List<ServiceInstance> instances = instanceRegistry.getOnlineInstances();
        return Map.of(
            "success", true,
            "data", instances
        );
    }

    @Data
    public static class RegistrationRequest {
        private String instanceId;
        private String serviceName;
        private String address;
        private List<ServiceInstance.ToolInfo> tools;
    }

    @Data
    public static class HeartbeatRequest {
        private String instanceId;
        private Long timestamp;
    }
}
