package com.nanobot.admin.service;

import com.nanobot.admin.domain.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实例注册表 - 管理所有注册的服务实例
 */
@Slf4j
@Service
public class InstanceRegistry {

    private final Map<String, ServiceInstance> instances = new ConcurrentHashMap<>();

    /**
     * 注册实例
     */
    public void register(ServiceInstance instance) {
        instance.setRegisterTime(System.currentTimeMillis());
        instance.setLastHeartbeat(System.currentTimeMillis());
        instance.setStatus("ONLINE");
        instances.put(instance.getInstanceId(), instance);
        log.info("Instance registered: {} at {}", instance.getInstanceId(), instance.getAddress());
    }

    /**
     * 更新心跳
     */
    public void heartbeat(String instanceId) {
        ServiceInstance instance = instances.get(instanceId);
        if (instance != null) {
            instance.setLastHeartbeat(System.currentTimeMillis());
            instance.setStatus("ONLINE");
        }
    }

    /**
     * 获取所有实例
     */
    public List<ServiceInstance> getAllInstances() {
        return new ArrayList<>(instances.values());
    }

    /**
     * 获取在线实例
     */
    public List<ServiceInstance> getOnlineInstances() {
        return instances.values().stream()
            .filter(i -> "ONLINE".equals(i.getStatus()))
            .toList();
    }

    /**
     * 定时检查实例状态（每 30 秒）
     */
    @Scheduled(fixedRate = 30000)
    public void checkInstanceStatus() {
        long now = System.currentTimeMillis();
        long timeout = 90000; // 90 秒超时

        instances.values().forEach(instance -> {
            if (now - instance.getLastHeartbeat() > timeout) {
                if ("ONLINE".equals(instance.getStatus())) {
                    instance.setStatus("OFFLINE");
                    log.warn("Instance marked as OFFLINE: {}", instance.getInstanceId());
                }
            }
        });
    }
}
