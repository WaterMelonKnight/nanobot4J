package com.nanobot.admin.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务实例信息
 */
@Data
public class ServiceInstance {

    /**
     * 实例 ID
     */
    private String instanceId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 实例地址
     */
    private String address;

    /**
     * 工具列表
     */
    private List<ToolInfo> tools = new ArrayList<>();

    /**
     * 状态：ONLINE, OFFLINE
     */
    private String status = "ONLINE";

    /**
     * 注册时间
     */
    private long registerTime;

    /**
     * 最后心跳时间
     */
    private long lastHeartbeat;

    @Data
    public static class ToolInfo {
        private String name;
        private String description;
        private String parameterSchema;
    }
}
