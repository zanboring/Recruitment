package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.service.AIService;
import com.example.recruitment.service.LocalModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI模型路由控制器
 * 负责根据用户选择和模型状态智能分发请求
 */
@Slf4j
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
@Tag(name = "模型管理", description = "小模型状态管理和路由控制")
public class ModelController {

    private final AIService aiService;
    private final LocalModelService localModelService;

    /**
     * 获取当前可用的模型列表及状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取模型状态", description = "获取主API和小模型的健康状态")
    public Result<Map<String, Object>> getModelStatus() {
        Map<String, Object> status = new HashMap<>();

        // 主API状态
        Map<String, Object> primaryStatus = new HashMap<>();
        primaryStatus.put("name", "ZhipuAI");
        primaryStatus.put("available", aiService.isApiKeyConfigured());
        primaryStatus.put("description", aiService.isApiKeyConfigured() ? "API Key已配置" : "未配置API Key");
        status.put("primary", primaryStatus);

        // 小模型状态
        Map<String, Object> localStatus = new HashMap<>();
        localStatus.put("name", "Local Model");
        localStatus.put("version", localModelService.getVersion());
        localStatus.put("available", localModelService.isAvailable());
        localStatus.put("status", localModelService.getHealthStatus().getStatus());
        localStatus.put("description", localModelService.getHealthStatus().getDescription());
        status.put("local", localStatus);

        return Result.success(status);
    }

    /**
     * 调用AI服务（智能路由）
     * 根据用户选择和模型状态自动选择使用哪个服务
     *
     * @param request 请求体，包含message和useLocalModel字段
     */
    @PostMapping("/chat")
    @Operation(summary = "AI聊天（智能路由）", description = "根据用户选择和模型状态智能分发请求")
    public Result<Map<String, Object>> chat(@RequestBody ModelChatRequest request) {
        String message = request.getMessage();
        boolean useLocalModel = Boolean.TRUE.equals(request.getUseLocalModel());

        // 空值检查
        if (message == null || message.trim().isEmpty()) {
            return Result.failed("消息内容不能为空");
        }

        Map<String, Object> result = new HashMap<>();
        String usedModel = null;
        String response = null;

        log.info("AI聊天请求 - 消息: {}, 偏好本地模型: {}", message, useLocalModel);

        try {
            if (useLocalModel && localModelService.isAvailable()) {
                // 用户选择使用小模型且小模型可用
                response = localModelService.generateResponse("", message);
                usedModel = "local";
                log.info("使用本地模型响应请求");
            } else if (aiService.isApiKeyConfigured()) {
                // 使用主API
                response = aiService.chatSync("", message);
                usedModel = "primary";
                log.info("使用主API响应请求");
            } else if (localModelService.isAvailable()) {
                // 主API不可用，回退到小模型
                response = localModelService.generateResponse("", message);
                usedModel = "local_fallback";
                log.warn("主API不可用，自动回退到本地模型");
            } else {
                // 所有服务都不可用
                response = "抱歉，当前AI服务暂时不可用，请稍后重试。";
                usedModel = "none";
                log.error("所有AI服务均不可用");
            }

            result.put("response", response);
            result.put("usedModel", usedModel);
            result.put("success", true);

            return Result.success(result);

        } catch (Exception e) {
            log.error("AI聊天失败: {}", e.getMessage(), e);
            
            // 尝试回退
            if (!"local".equals(usedModel) && localModelService.isAvailable()) {
                try {
                    response = localModelService.generateResponse("", message);
                    usedModel = "local_fallback";
                    result.put("response", response);
                    result.put("usedModel", usedModel);
                    result.put("success", true);
                    result.put("fallback", true);
                    log.warn("主API失败，回退到本地模型成功");
                    return Result.success(result);
                } catch (Exception fallbackEx) {
                    log.error("回退到本地模型也失败: {}", fallbackEx.getMessage());
                }
            }

            return Result.failed("AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 小模型健康检查接口
     */
    @GetMapping("/health")
    @Operation(summary = "小模型健康检查", description = "检查本地小模型服务的健康状态")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("available", localModelService.isAvailable());
        health.put("status", localModelService.getHealthStatus().getStatus());
        health.put("description", localModelService.getHealthStatus().getDescription());
        health.put("version", localModelService.getVersion());
        health.put("timestamp", System.currentTimeMillis());

        return Result.success(health);
    }

    /**
     * 请求体定义
     */
    public static class ModelChatRequest {
        private String message;
        private Boolean useLocalModel;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Boolean getUseLocalModel() {
            return useLocalModel;
        }

        public void setUseLocalModel(Boolean useLocalModel) {
            this.useLocalModel = useLocalModel;
        }
    }
}