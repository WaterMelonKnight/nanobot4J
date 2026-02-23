package com.nanobot.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.admin.domain.ServiceInstance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 远程工具执行器 - 通过 HTTP RPC 调用 Client 端的工具
 */
@Slf4j
@Service
public class RemoteToolExecutor {

    private final InstanceRegistry instanceRegistry;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RemoteToolExecutor(InstanceRegistry instanceRegistry) {
        this.instanceRegistry = instanceRegistry;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 执行远程工具调用
     * @param toolName 工具名称
     * @param params 工具参数
     * @return 执行结果
     */
    public String executeRemoteTool(String toolName, Map<String, Object> params) {
        log.info("Executing remote tool: {} with params: {}", toolName, params);

        // 1. 查找注册了该工具的在线实例
        ServiceInstance instance = findInstanceWithTool(toolName);
        if (instance == null) {
            log.error("No online instance found with tool: {}", toolName);
            return "错误：找不到提供该工具的服务实例";
        }

        // 2. 构建远程调用 URL
        String url = instance.getAddress() + "/api/nanobot/client/execute";
        log.info("Calling remote instance: {} at {}", instance.getInstanceId(), url);

        // 3. 构建请求体
        ToolExecutionRequest request = new ToolExecutionRequest();
        request.setToolName(toolName);
        request.setParams(params);

        try {
            // 4. 发起 HTTP POST 请求
            String requestBody = objectMapper.writeValueAsString(request);
            RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            // 5. 同步等待响应
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Remote tool execution failed with HTTP {}", response.code());
                    return "错误：远程调用失败 (HTTP " + response.code() + ")";
                }

                String responseBody = response.body().string();
                ToolExecutionResponse toolResponse = objectMapper.readValue(
                        responseBody,
                        ToolExecutionResponse.class
                );

                // 6. 检查执行结果
                if (toolResponse.getCode() == 200) {
                    Object data = toolResponse.getData();
                    String result = data != null ? data.toString() : "";
                    log.info("Remote tool execution succeeded: {}", result);
                    return result;
                } else {
                    log.error("Remote tool execution failed: {}", toolResponse.getMessage());
                    return "错误：" + toolResponse.getMessage();
                }
            }

        } catch (IOException e) {
            log.error("Failed to execute remote tool: {}", toolName, e);
            return "错误：远程调用异常 - " + e.getMessage();
        }
    }

    /**
     * 查找注册了指定工具的在线实例
     */
    private ServiceInstance findInstanceWithTool(String toolName) {
        List<ServiceInstance> onlineInstances = instanceRegistry.getOnlineInstances();

        for (ServiceInstance instance : onlineInstances) {
            if (instance.getTools() != null) {
                for (ServiceInstance.ToolInfo tool : instance.getTools()) {
                    if (toolName.equals(tool.getName())) {
                        return instance;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 工具执行请求
     */
    @Data
    private static class ToolExecutionRequest {
        private String toolName;
        private Map<String, Object> params;
    }

    /**
     * 工具执行响应
     */
    @Data
    private static class ToolExecutionResponse {
        private int code;
        private Object data;
        private String message;
    }
}
