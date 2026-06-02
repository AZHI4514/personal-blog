package com.azhi.service.roomagent;

import com.azhi.pojo.RoomAgentConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoomAgentChatService {

    private final RoomAgentConfigService configService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RoomAgentChatService(RoomAgentConfigService configService, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public Map<String, Object> callAllowedTool(Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", ""));
        if (!"understand_image".equals(name) && !"web_search".equals(name)) {
            throw new IllegalArgumentException("tool not allowed");
        }
        return Map.of("text", "template mcp result for " + name, "raw", body);
    }

    public Map<String, Object> chat(Map<String, Object> body) {
        RoomAgentConfig config = configService.getCurrentConfig();
        if (isBlank(config.getApiUrl()) || isBlank(config.getApiKey()) || isBlank(config.getModel())) {
            throw new IllegalStateException("llm config incomplete");
        }
        try {
            Map<String, Object> payload = buildChatPayload(config, body);
            String responseText = postJson(config.getApiUrl(), config.getApiKey(), objectMapper.writeValueAsString(payload));
            JsonNode root = objectMapper.readTree(responseText);
            String reply = extractReply(root);
            if (reply.isBlank()) {
                throw new IllegalStateException("empty llm reply");
            }
            return Map.of("reply", reply, "raw", root);
        } catch (Exception e) {
            throw new IllegalStateException("chat request failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildChatPayload(RoomAgentConfig config, Map<String, Object> body) {
        String message = String.valueOf(body.getOrDefault("message", ""));
        String systemPrompt = String.valueOf(body.getOrDefault("systemPrompt", ""));
        List<Map<String, Object>> messages = new ArrayList<>();
        if (!isBlank(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        Object conversation = body.get("conversation");
        if (conversation instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    Object roleValue = map.get("role");
                    Object contentValue = map.get("content");
                    String role = roleValue == null ? "user" : String.valueOf(roleValue);
                    String content = contentValue == null ? "" : String.valueOf(contentValue);
                    messages.add(Map.of("role", role, "content", content));
                }
            }
        }

        Object image = body.get("image");
        if (image instanceof Map<?, ?> imageMap && imageMap.get("dataUrl") != null) {
            messages.add(Map.of(
                    "role", "user",
                    "content", List.of(
                            Map.of("type", "text", "text", isBlank(message) ? "Please describe this image." : message),
                            Map.of("type", "image_url", "image_url", Map.of("url", String.valueOf(imageMap.get("dataUrl"))))
                    )
            ));
        } else {
            messages.add(Map.of("role", "user", "content", message));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", config.getModel());
        payload.put("messages", messages);
        return payload;
    }

    private String postJson(String apiUrl, String apiKey, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("llm status " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private String extractReply(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode contentNode = choices.get(0).path("message").path("content");
            if (contentNode.isTextual()) {
                return contentNode.asText();
            }
            if (contentNode.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode item : contentNode) {
                    if (item.path("type").asText("").equals("text")) {
                        builder.append(item.path("text").asText(""));
                    }
                }
                return builder.toString();
            }
        }
        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual()) {
            return outputText.asText();
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
