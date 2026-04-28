package com.example.recruitment.service;

/**
 * 本地小模型服务接口
 * 提供与主AI服务一致的接口，支持无缝切换
 */
public interface LocalModelService {

    /**
     * 检查小模型服务是否可用
     * @return true表示可用，false表示不可用
     */
    boolean isAvailable();

    /**
     * 获取小模型健康状态详情
     * @return 健康状态信息
     */
    HealthStatus getHealthStatus();

    /**
     * 调用小模型生成分析报告
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return AI生成的响应内容
     */
    String generateResponse(String systemPrompt, String userMessage);

    /**
     * 获取小模型版本信息
     * @return 版本字符串
     */
    String getVersion();

    /**
     * 健康状态枚举
     */
    enum HealthStatus {
        HEALTHY("健康", "小模型服务运行正常"),
        DEGRADED("降级", "小模型服务性能下降"),
        UNHEALTHY("异常", "小模型服务不可用"),
        UNKNOWN("未知", "无法获取状态");

        private final String status;
        private final String description;

        HealthStatus(String status, String description) {
            this.status = status;
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public String getDescription() {
            return description;
        }
    }
}