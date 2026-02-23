package com.nanobot.starter.controller;

import lombok.Data;

/**
 * 工具执行响应
 */
@Data
public class ToolExecutionResponse {
    /**
     * 响应码：200 成功，500 失败
     */
    private int code;

    /**
     * 执行结果数据
     */
    private Object data;

    /**
     * 响应消息
     */
    private String message;

    public static ToolExecutionResponse success(Object data) {
        ToolExecutionResponse response = new ToolExecutionResponse();
        response.setCode(200);
        response.setData(data);
        response.setMessage("success");
        return response;
    }

    public static ToolExecutionResponse failure(String message) {
        ToolExecutionResponse response = new ToolExecutionResponse();
        response.setCode(500);
        response.setData(null);
        response.setMessage(message);
        return response;
    }
}
