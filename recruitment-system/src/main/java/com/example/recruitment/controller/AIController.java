package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI分析", description = "AI大模型问答接口")
public class AIController {

    @Value("${zhipuai.api.key:}")
    private String apiKey;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chat")
    @Operation(summary = "AI聊天", description = "通过ZhipuAI进行智能问答")
    public Result<String> chat(@RequestBody AIChatRequest request) {
        log.info("AI聊天请求: message={}", request.getMessage());
        try {
            String response = callZhipuAI(request.getMessage());
            log.info("AI聊天响应: {}", response);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI聊天失败", e);
            return Result.failed("AI聊天失败: " + e.getMessage());
        }
    }

    @PostMapping("/stream")
    @Operation(summary = "AI流式聊天", description = "通过ZhipuAI进行流式智能问答")
    public SseEmitter streamChat(@RequestBody AIChatRequest request) {
        SseEmitter emitter = new SseEmitter(120000L);
        String message = request.getMessage();

        executor.execute(() -> {
            try {
                callZhipuAIStream(message, emitter);
                emitter.complete();
            } catch (Exception e) {
                log.error("流式聊天失败", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private String callZhipuAI(String message) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置智谱AI API Key，请设置 zhipuai.api.key 或环境变量 ZHIPUAI_API_KEY");
        }
        String url = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Accept", "application/json; charset=UTF-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(45000);

        String requestBody = buildRequestBody(message, false);

        log.info("API请求URL: {}", url);
        log.info("API请求体: {}", requestBody);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        log.info("API响应码: {}", responseCode);

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        responseCode == HttpURLConnection.HTTP_OK ?
                                connection.getInputStream() : connection.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        String responseStr = response.toString();
        log.info("API响应: {}", responseStr);

        if (responseStr.contains("error")) {
            throw new Exception("API调用失败: " + responseStr);
        }

        return parseResponse(responseStr);
    }

    private void callZhipuAIStream(String message, SseEmitter emitter) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置智谱AI API Key，请设置 zhipuai.api.key 或环境变量 ZHIPUAI_API_KEY");
        }
        String url = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(120000);

        String requestBody = buildRequestBody(message, true);

        log.info("流式API请求URL: {}", url);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        log.info("流式API响应码: {}", responseCode);
        if (responseCode != HttpURLConnection.HTTP_OK) {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
            }
            throw new Exception("流式API调用失败: " + responseCode + ", " + errorResponse);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if (data.equals("[DONE]")) {
                        break;
                    }
                    String content = extractContentFromStreamData(data);
                    if (content != null && !content.isEmpty()) {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(content));
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("读取流式响应失败", e);
            throw e;
        }
    }

    private String buildRequestBody(String message, boolean stream) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "glm-4");
        payload.put("messages", List.of(Map.of("role", "user", "content", message)));
        if (stream) {
            payload.put("stream", true);
        }
        return objectMapper.writeValueAsString(payload);
    }

    private String extractContentFromStreamData(String data) {
        try {
            int contentStart = data.indexOf("\"content\":\"");
            if (contentStart != -1) {
                contentStart += 11;
                int contentEnd = contentStart;
                int length = data.length();
                boolean escaped = false;
                while (contentEnd < length) {
                    char c = data.charAt(contentEnd);
                    if (c == '\\') {
                        escaped = true;
                        contentEnd++;
                    } else if (c == '"' && !escaped) {
                        break;
                    } else {
                        contentEnd++;
                        escaped = false;
                    }
                }
                if (contentEnd > contentStart) {
                    String content = data.substring(contentStart, contentEnd);
                    content = content.replace("\\n", "\n");
                    content = content.replace("\\\"", "\"");
                    content = content.replace("\\\\", "\\");
                    return content;
                }
            }
        } catch (Exception e) {
            log.error("解析流式数据失败: {}", data, e);
        }
        return "";
    }

    private String parseResponse(String response) {
        try {
            int choicesStart = response.indexOf("choices");
            if (choicesStart != -1) {
                int messageStart = response.indexOf("message", choicesStart);
                if (messageStart != -1) {
                    int contentStart = response.indexOf("content", messageStart);
                    if (contentStart != -1) {
                        int valueStart = response.indexOf("\"", contentStart + 8);
                        if (valueStart != -1) {
                            int valueEnd = valueStart + 1;
                            int length = response.length();
                            boolean escaped = false;
                            while (valueEnd < length) {
                                char c = response.charAt(valueEnd);
                                if (c == '\\' && valueEnd + 1 < length) {
                                    valueEnd += 2;
                                    escaped = true;
                                } else if (c == '\"' && !escaped) {
                                    break;
                                } else {
                                    valueEnd++;
                                    escaped = false;
                                }
                            }

                            if (valueEnd < length) {
                                String content = response.substring(valueStart + 1, valueEnd);
                                content = content.replace("\\n", "\n");
                                content = content.replace("\\\"", "\"");
                                return content;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析响应失败", e);
        }
        return response;
    }

    public static class AIChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
