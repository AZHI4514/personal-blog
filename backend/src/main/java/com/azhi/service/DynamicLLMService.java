package com.azhi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 动态 LLM 服务：使用 Java 内置 HttpClient 直接调用 OpenAI 兼容 API，
 * 避免 LangChain4j ServiceLoader 在动态创建 OpenAiChatModel 时的
 * HTTP client 冲突（jdk vs spring-restclient）。
 *
 * 注意：不影响 Spring Boot 自动配置的 Mimo 聊天模型（AiCodeHelperServiceImpl 使用）。
 */
@Service
public class DynamicLLMService {

    private static final Logger log = LoggerFactory.getLogger(DynamicLLMService.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用 LLM 生成文本。
     */
    @SuppressWarnings("unchecked")
    public String generate(Long userId, String baseUrl, String apiKey, String modelName,
                           String systemPrompt, String userPrompt) {
        String url = normalizeUrl(baseUrl) + "/chat/completions";

        Map<String, Object> requestBody = Map.of(
            "model", modelName != null && !modelName.isBlank() ? modelName : "gpt-3.5-turbo",
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", 0.8,
            "max_tokens", 1024
        );

        try {
            String json = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String errorBody = response.body();
                if (errorBody != null && errorBody.length() > 500) {
                    errorBody = errorBody.substring(0, 500);
                }
                throw new RuntimeException("AI 接口返回 HTTP " + response.statusCode() + ": " + errorBody);
            }

            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("AI 返回了空响应");
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                throw new RuntimeException("AI 响应格式异常");
            }
            return (String) message.get("content");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("AI 生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式调用 LLM 生成文本。每收到一个内容块就调用 onChunk 回调，
     * 返回完整的拼接后文本。
     */
    @SuppressWarnings("unchecked")
    public String generateStream(Long userId, String baseUrl, String apiKey, String modelName,
                                  String systemPrompt, String userPrompt,
                                  Consumer<String> onChunk) throws IOException, InterruptedException {
        String url = normalizeUrl(baseUrl) + "/chat/completions";

        Map<String, Object> requestBody = Map.of(
            "model", modelName != null && !modelName.isBlank() ? modelName : "gpt-3.5-turbo",
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", 0.8,
            "max_tokens", 1024,
            "stream", true
        );

        String json = objectMapper.writeValueAsString(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .timeout(Duration.ofSeconds(120))
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            String errorBody;
            try (java.io.InputStream errStream = response.body()) {
                errorBody = new String(errStream.readAllBytes());
                if (errorBody.length() > 500) errorBody = errorBody.substring(0, 500);
            }
            throw new RuntimeException("AI 接口返回 HTTP " + response.statusCode() + ": " + errorBody);
        }

        StringBuilder fullContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                            if (delta != null) {
                                String content = (String) delta.get("content");
                                if (content != null) {
                                    fullContent.append(content);
                                    if (onChunk != null) {
                                        onChunk.accept(content);
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                        // 跳过无法解析的 SSE 块
                    }
                }
            }
        }

        if (fullContent.isEmpty()) {
            throw new RuntimeException("AI 返回了空响应");
        }
        return fullContent.toString();
    }

    /**
     * 测试连接：发送简单请求验证配置是否可用。
     */
    @SuppressWarnings("unchecked")
    public String testConnection(String baseUrl, String apiKey, String modelName) {
        String url = normalizeUrl(baseUrl) + "/chat/completions";

        Map<String, Object> requestBody = Map.of(
            "model", modelName != null && !modelName.isBlank() ? modelName : "gpt-3.5-turbo",
            "messages", List.of(
                Map.of("role", "user", "content", "请回复OK（只回复这两个字母，不要回复其他内容）。")
            ),
            "temperature", 0.1,
            "max_tokens", 10
        );

        try {
            String json = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(TEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String errorBody = response.body();
                if (errorBody != null && errorBody.length() > 300) {
                    errorBody = errorBody.substring(0, 300);
                }
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + errorBody);
            }

            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "连接成功但模型返回了空响应，请检查模型名称是否正确";
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = message != null ? (String) message.get("content") : "";
            return "连接测试成功！模型返回: " + content;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("连接测试失败: " + e.getMessage(), e);
        }
    }

    /** 确保 URL 不以 / 结尾 */
    private String normalizeUrl(String baseUrl) {
        if (baseUrl == null) return "";
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /** 清除缓存（保留接口兼容性，当前实现无缓存） */
    public void evictModel(Long userId) {
        // 无缓存需要清除，方法保留供 LlmConfigServiceImpl 调用
    }
}
