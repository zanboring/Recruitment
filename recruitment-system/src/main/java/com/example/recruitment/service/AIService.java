package com.example.recruitment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智谱AI统一服务类
 * 
 * 封装与智谱AI大模型（GLM-4）的所有交互逻辑，包括：
 * - 同步调用
 * - 流式调用（SSE）
 * - 统一的请求构建和响应解析
 * 
 * 使用方法：
 * 1. 注入本服务
 * 2. 调用 chatSync(systemPrompt, userMessage) 获取同步响应
 * 3. 调用 chatStream(systemPrompt, userMessage, emitter) 获取流式响应
 */
@Service
@Slf4j
public class AIService {

    @Value("${zhipuai.api.key:}")
    private String apiKey;

    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final String DEFAULT_MODEL = "glm-4";
    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_SYNC_MS = 120000;
    private static final int READ_TIMEOUT_STREAM_MS = 180000;
    private static final int MAX_RETRY_COUNT = 2;
    private static final long RETRY_DELAY_MS = 2000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查API Key是否已配置
     */
    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 同步调用智谱AI（带重试机制）
     *
     * @param systemPrompt 系统提示词（定义AI角色和行为）
     * @param userMessage 用户消息
     * @return AI的回复内容
     * @throws Exception 调用失败时抛出异常
     */
    public String chatSync(String systemPrompt, String userMessage) throws Exception {
        if (!isApiKeyConfigured()) {
            throw new IllegalStateException("未配置智谱AI API Key，请设置环境变量 ZHIPUAI_API_KEY");
        }

        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                return chatSyncInternal(systemPrompt, userMessage);
            } catch (Exception e) {
                lastException = e;
                log.warn("AI调用第{}次失败: {}", attempt, e.getMessage());
                
                if (attempt < MAX_RETRY_COUNT) {
                    log.info("等待{}ms后进行第{}次重试...", RETRY_DELAY_MS, attempt + 1);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }
        
        throw new Exception("AI调用失败，已重试" + MAX_RETRY_COUNT + "次: " + lastException.getMessage(), lastException);
    }

    /**
     * 内部同步调用方法（不带重试）
     */
    private String chatSyncInternal(String systemPrompt, String userMessage) throws Exception {
        HttpURLConnection connection = createConnection(false);
        String requestBody = buildRequestBody(systemPrompt, userMessage, false);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readErrorStream(connection);
            throw new Exception("API调用失败 (HTTP " + responseCode + "): " + errorBody);
        }

        String responseBody = readResponseStream(connection);
        return parseSyncResponse(responseBody);
    }

    /**
     * 流式调用智谱AI（SSE）
     * 采用行缓冲模式：将收到的token先缓存到当前行，达到换行或缓存满时一次性发送
     * 这样避免前端每个token都换行显示的问题
     */
    public void chatStream(String systemPrompt, String userMessage, SseEmitter emitter) throws Exception {
        if (!isApiKeyConfigured()) {
            throw new IllegalStateException("未配置智谱AI API Key，请设置环境变量 ZHIPUAI_API_KEY");
        }

        HttpURLConnection connection = createConnection(true);
        String requestBody = buildRequestBody(systemPrompt, userMessage, true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readErrorStream(connection);
            throw new Exception("流式API调用失败 (HTTP " + responseCode + "): " + errorBody);
        }

        final int MAX_LINE_BUFFER = 150; // 每150字符强制发送一行

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            StringBuilder currentLine = new StringBuilder();

            while ((line = br.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if (data.equals("[DONE]")) {
                        // 发送最后一行（如果有内容）
                        if (currentLine.length() > 0) {
                            String toSend = currentLine.toString().trim();
                            if (!toSend.isEmpty()) {
                                emitter.send(SseEmitter.event().name("message").data(toSend));
                            }
                        }
                        break;
                    }
                    String content = extractStreamContent(data);
                    if (content != null && !content.isEmpty()) {
                        // 清理：移除换行符和多余空格
                        String cleaned = content.replace("\n", "").replace("\r", "").replaceAll("\\s+", " ");
                        currentLine.append(cleaned);

                        // 逻辑：收集内容直到遇到句号/问号/感叹号，或者缓冲满
                        // 这样每个完整句子作为一行发送
                        if (currentLine.length() >= MAX_LINE_BUFFER) {
                            String toSend = currentLine.toString().trim();
                            if (!toSend.isEmpty()) {
                                emitter.send(SseEmitter.event().name("message").data(toSend + "\n"));
                            }
                            currentLine.setLength(0);
                        }
                    }
                }
            }
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 创建HttpURLConnection连接
     */
    private HttpURLConnection createConnection(boolean streamMode) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Accept", streamMode ? "text/event-stream" : "application/json; charset=UTF-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(streamMode ? READ_TIMEOUT_STREAM_MS : READ_TIMEOUT_SYNC_MS);
        return connection;
    }

    /**
     * 构建JSON请求体
     */
    private String buildRequestBody(String systemPrompt, String userMessage, boolean stream) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", DEFAULT_MODEL);

        List<Map<String, String>> messages = new ArrayList<>();
        
        // 添加System Prompt
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }

        // 添加用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        payload.put("messages", messages);
        if (stream) {
            payload.put("stream", true);
        }
        return objectMapper.writeValueAsString(payload);
    }

    /**
     * 解析同步响应，提取content字段
     */
    private String parseSyncResponse(String responseBody) {
        try {
            if (responseBody == null || responseBody.isBlank()) {
                log.warn("响应体为空");
                return "";
            }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode firstChoice = choices.get(0);
                if (firstChoice != null) {
                    JsonNode message = firstChoice.path("message");
                    if (message != null && !message.isMissingNode()) {
                        return message.path("content").asText("");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("标准JSON解析失败: {}", e.getMessage());
        }
        log.error("无法解析AI响应体");
        return "";
    }

    /**
     * 从流式数据中提取content
     */
    private String extractStreamContent(String data) {
        try {
            JsonNode node = objectMapper.readTree(data);
            String content = node.path("choices").path(0).path("delta").path("content").asText("");
            if (content != null && !content.isEmpty()) {
                // 过滤掉换行符，避免前端每个词都换行
                content = content.replace("\n", "").replace("\r", "");
                if (!content.isEmpty()) {
                    return content;
                }
            }
        } catch (Exception e) {
            log.debug("流式JSON解析异常: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 读取正常响应流
     */
    private String readResponseStream(HttpURLConnection connection) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * 读取错误响应流
     */
    private String readErrorStream(HttpURLConnection connection) throws Exception {
        StringBuilder error = new StringBuilder();
        if (connection.getErrorStream() != null) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    error.append(line);
                }
            }
        }
        return error.toString();
    }
}
