package com.example.recruitment.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Ollama本地大模型服务接口
 * 提供与本地Ollama服务的交互能力
 */
public interface OllamaService {

    /**
     * 检查Ollama服务是否可用
     * @return true表示服务可用，false表示不可用
     */
    boolean isAvailable();

    /**
     * 获取当前加载的模型名称
     * @return 模型名称，如 "qwen2:7b"
     */
    String getModelName();

    /**
     * 同步调用Ollama生成响应
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return AI生成的响应内容
     * @throws Exception 调用失败时抛出异常
     */
    String chatSync(String systemPrompt, String userMessage) throws Exception;

    /**
     * 流式调用Ollama（SSE）
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param emitter SSE发射器
     * @throws Exception 调用失败时抛出异常
     */
    void chatStream(String systemPrompt, String userMessage, SseEmitter emitter) throws Exception;

    /**
     * 获取Ollama服务健康状态描述
     * @return 健康状态描述
     */
    String getHealthDescription();
}
