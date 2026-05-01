package com.example.recruitment.service.impl;

import com.example.recruitment.service.OllamaService;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ollama本地大模型服务实现
 * 通过HTTP调用本地Ollama服务
 */
@Service
@Slf4j
public class OllamaServiceImpl implements OllamaService {

    @Value("${ollama.enabled:false}")
    private boolean enabled;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:qwen2:7b}")
    private String model;

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_SYNC_MS = 120000;
    private static final int READ_TIMEOUT_STREAM_MS = 180000;
    private static final int MAX_LINE_BUFFER = 150;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean availableCache = new AtomicBoolean(false);
    private volatile long lastCheckTime = 0;
    private static final long CACHE_TTL_MS = 30000;

    private static final String LOCAL_SYSTEM_PROMPT = """
            你是招聘数据分析助手，基于本地知识库回答问题。

            回答要点：
            - 简洁直接，不要太长的回复
            - 如果不确定，直接说"这个我不太清楚，建议使用主API获取更详细分析"
            - 优先使用真实数据，数据不足时说明
            - 使用中文回答
            """;

    @Override
    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastCheckTime < CACHE_TTL_MS) {
            return availableCache.get();
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) 
                URI.create(baseUrl + "/api/tags").toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            boolean available = responseCode == 200;
            
            availableCache.set(available);
            lastCheckTime = now;
            
            if (available) {
                log.debug("Ollama服务可用，模型: {}", model);
            } else {
                log.warn("Ollama服务响应异常: HTTP {}", responseCode);
            }
            
            return available;
        } catch (Exception e) {
            log.debug("Ollama服务不可用: {}", e.getMessage());
            availableCache.set(false);
            lastCheckTime = now;
            return false;
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public String chatSync(String systemPrompt, String userMessage) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Ollama服务不可用");
        }

        HttpURLConnection connection = createConnection(false);
        String requestBody = buildRequestBody(systemPrompt, userMessage, false);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readErrorStream(connection);
            throw new Exception("Ollama调用失败 (HTTP " + responseCode + "): " + errorBody);
        }

        String responseBody = readResponseStream(connection);
        return parseSyncResponse(responseBody);
    }

    @Override
    public void chatStream(String systemPrompt, String userMessage, SseEmitter emitter) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Ollama服务不可用");
        }

        HttpURLConnection connection = createConnection(true);
        String requestBody = buildRequestBody(systemPrompt, userMessage, true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readErrorStream(connection);
            throw new Exception("Ollama流式调用失败 (HTTP " + responseCode + "): " + errorBody);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            StringBuilder currentLine = new StringBuilder();

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String content = extractStreamContent(line);
                if (content != null && !content.isEmpty()) {
                    currentLine.append(content);

                    if (currentLine.length() >= MAX_LINE_BUFFER) {
                        String toSend = currentLine.toString();
                        if (!toSend.isEmpty()) {
                            emitter.send(SseEmitter.event().name("message").data(toSend));
                        }
                        currentLine.setLength(0);
                    }
                }
            }

            if (currentLine.length() > 0) {
                String toSend = currentLine.toString();
                if (!toSend.isEmpty()) {
                    emitter.send(SseEmitter.event().name("message").data(toSend));
                }
            }
        }
    }

    @Override
    public String getHealthDescription() {
        if (!enabled) {
            return "Ollama服务未启用";
        }
        if (isAvailable()) {
            return "Ollama服务运行正常，模型: " + model;
        }
        return "Ollama服务不可用，请确保Ollama已启动";
    }

    private HttpURLConnection createConnection(boolean streamMode) throws Exception {
        String url = baseUrl + "/api/chat";
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", streamMode ? "application/x-ndjson" : "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(streamMode ? READ_TIMEOUT_STREAM_MS : READ_TIMEOUT_SYNC_MS);
        return connection;
    }

    private String buildRequestBody(String systemPrompt, String userMessage, boolean stream) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();
        
        String effectivePrompt = (systemPrompt != null && !systemPrompt.isBlank()) 
            ? systemPrompt : LOCAL_SYSTEM_PROMPT;
        
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", effectivePrompt);
        messages.add(sysMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        payload.put("messages", messages);
        payload.put("stream", stream);
        
        return objectMapper.writeValueAsString(payload);
    }

    private String parseSyncResponse(String responseBody) {
        try {
            if (responseBody == null || responseBody.isBlank()) {
                log.warn("Ollama响应体为空");
                return "";
            }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode message = root.path("message");
            if (message != null && !message.isMissingNode()) {
                return message.path("content").asText("");
            }
        } catch (Exception e) {
            log.warn("解析Ollama响应失败: {}", e.getMessage());
        }
        return "";
    }

    private String extractStreamContent(String line) {
        try {
            JsonNode node = objectMapper.readTree(line);
            boolean done = node.path("done").asBoolean(false);
            if (done) {
                return null;
            }
            return node.path("message").path("content").asText("");
        } catch (Exception e) {
            log.debug("解析Ollama流式响应失败: {}", e.getMessage());
            return "";
        }
    }

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
