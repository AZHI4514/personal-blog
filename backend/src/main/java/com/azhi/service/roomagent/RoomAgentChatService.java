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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomAgentChatService {
    private static final String DEFAULT_MCP_PROTOCOL_VERSION = "2025-03-26";

    private final RoomAgentConfigService configService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AtomicLong rpcId = new AtomicLong(1);
    private final Map<String, McpSessionState> mcpSessions = new ConcurrentHashMap<>();

    public RoomAgentChatService(RoomAgentConfigService configService, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public Map<String, Object> callAllowedTool(Map<String, Object> body) {
        RoomAgentConfig config = configService.getCurrentConfig();
        if (config.getMcpEnabled() == null || !config.getMcpEnabled()) {
            throw new IllegalStateException("mcp disabled");
        }
        if (isBlank(config.getMcpEndpoint())) {
            throw new IllegalStateException("mcp endpoint missing");
        }
        String name = String.valueOf(body.getOrDefault("name", ""));
        Set<String> allowlist = parseAllowlist(config.getMcpToolAllowlist());
        if (!allowlist.contains(name)) {
            throw new IllegalArgumentException("tool not allowed");
        }
        try {
            Object args = body.get("args");
            Map<String, Object> toolArgs = Map.of();
            if (args instanceof Map<?, ?>) {
                toolArgs = castMap((Map<?, ?>) args);
            }
            Map<String, Object> result = callMcpToolWithRetry(config.getMcpEndpoint(), name, toolArgs);
            return Map.of(
                    "text", extractMcpText(result),
                    "raw", result
            );
        } catch (Exception e) {
            throw new IllegalStateException("mcp request failed: " + e.getMessage(), e);
        }
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

    private Map<String, Object> callMcpToolWithRetry(String endpoint, String name, Map<String, Object> args) throws Exception {
        try {
            return callMcpTool(endpoint, name, args);
        } catch (IllegalStateException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("mcp status 404") || message.contains("mcp status 400")) {
                mcpSessions.remove(endpoint);
                return callMcpTool(endpoint, name, args);
            }
            throw e;
        }
    }

    private Map<String, Object> callMcpTool(String endpoint, String name, Map<String, Object> args) throws Exception {
        McpSessionState session = ensureMcpSession(endpoint);
        long id = rpcId.getAndIncrement();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("id", id);
        payload.put("method", "tools/call");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("_meta", buildMcpMeta(session.protocolVersion));
        params.put("name", name);
        params.put("arguments", args);
        payload.put("params", params);

        HttpResponse<String> response = postMcp(endpoint, objectMapper.writeValueAsString(payload), session, "tools/call", name);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("mcp status " + response.statusCode() + ": " + response.body());
        }

        String body = response.body();
        JsonNode root = parseMcpResponse(body);
        if (root.path("error").isObject()) {
            JsonNode error = root.path("error");
            throw new IllegalStateException(error.path("message").asText("mcp error"));
        }
        JsonNode result = root.path("result");
        if (!result.isObject()) {
            throw new IllegalStateException("invalid mcp result");
        }
        return objectMapper.convertValue(result, objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
    }

    private McpSessionState ensureMcpSession(String endpoint) throws Exception {
        McpSessionState current = mcpSessions.get(endpoint);
        if (current != null) {
            return current;
        }

        long id = rpcId.getAndIncrement();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("id", id);
        payload.put("method", "initialize");
        Map<String, Object> clientInfo = new LinkedHashMap<>();
        clientInfo.put("name", "personal-blog-room-agent");
        clientInfo.put("version", "1.0.0");
        Map<String, Object> initializeParams = new LinkedHashMap<>();
        initializeParams.put("protocolVersion", DEFAULT_MCP_PROTOCOL_VERSION);
        initializeParams.put("capabilities", new LinkedHashMap<>());
        initializeParams.put("clientInfo", clientInfo);
        payload.put("params", initializeParams);

        HttpResponse<String> initializeResponse = postMcp(endpoint, objectMapper.writeValueAsString(payload), null, "initialize", null);
        if (initializeResponse.statusCode() < 200 || initializeResponse.statusCode() >= 300) {
            throw new IllegalStateException("mcp initialize status " + initializeResponse.statusCode() + ": " + initializeResponse.body());
        }

        JsonNode root = parseMcpResponse(initializeResponse.body());
        if (root.path("error").isObject()) {
            JsonNode error = root.path("error");
            throw new IllegalStateException("mcp initialize error: " + error.path("message").asText("unknown error"));
        }

        String protocolVersion = root.path("result").path("protocolVersion").asText(DEFAULT_MCP_PROTOCOL_VERSION);
        String sessionId = firstHeader(initializeResponse, "Mcp-Session-Id");
        McpSessionState session = new McpSessionState(protocolVersion, sessionId);

        Map<String, Object> initializedPayload = new LinkedHashMap<>();
        initializedPayload.put("jsonrpc", "2.0");
        initializedPayload.put("method", "notifications/initialized");
        Map<String, Object> initializedParams = new LinkedHashMap<>();
        initializedParams.put("_meta", buildMcpMeta(protocolVersion));
        initializedPayload.put("params", initializedParams);

        HttpResponse<String> initializedResponse = postMcp(endpoint, objectMapper.writeValueAsString(initializedPayload), session, "notifications/initialized", null);
        if (initializedResponse.statusCode() >= 400) {
            throw new IllegalStateException("mcp initialized status " + initializedResponse.statusCode() + ": " + initializedResponse.body());
        }

        mcpSessions.put(endpoint, session);
        return session;
    }

    private HttpResponse<String> postMcp(String endpoint, String body, McpSessionState session, String method, String name) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream");
        if (session != null && !isBlank(session.protocolVersion)) {
            builder.header("MCP-Protocol-Version", session.protocolVersion);
        } else if (!"initialize".equals(method)) {
            builder.header("MCP-Protocol-Version", DEFAULT_MCP_PROTOCOL_VERSION);
        }
        if (!isBlank(method)) {
            builder.header("Mcp-Method", method);
        }
        if (!isBlank(name)) {
            builder.header("Mcp-Name", name);
        }
        if (session != null && !isBlank(session.sessionId)) {
            builder.header("Mcp-Session-Id", session.sessionId);
        }
        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private Map<String, Object> buildMcpMeta(String protocolVersion) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("io.modelcontextprotocol/protocolVersion", isBlank(protocolVersion) ? DEFAULT_MCP_PROTOCOL_VERSION : protocolVersion);
        Map<String, Object> clientInfo = new LinkedHashMap<>();
        clientInfo.put("name", "personal-blog-room-agent");
        clientInfo.put("version", "1.0.0");
        meta.put("io.modelcontextprotocol/clientInfo", clientInfo);
        meta.put("io.modelcontextprotocol/clientCapabilities", new LinkedHashMap<>());
        return meta;
    }

    private String firstHeader(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).orElse("");
    }

    private JsonNode parseMcpResponse(String body) throws Exception {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("empty mcp response");
        }
        if (trimmed.startsWith("data:")) {
            StringBuilder json = new StringBuilder();
            for (String line : trimmed.split("\\R")) {
                String current = line.trim();
                if (current.startsWith("data:")) {
                    String payload = current.substring(5).trim();
                    if (!payload.isEmpty() && !"[DONE]".equals(payload)) {
                        if (json.length() > 0) json.append('\n');
                        json.append(payload);
                    }
                }
            }
            if (json.length() == 0) {
                throw new IllegalStateException("empty mcp event payload");
            }
            return objectMapper.readTree(json.toString());
        }
        return objectMapper.readTree(trimmed);
    }

    private String extractMcpText(Map<String, Object> result) {
        JsonNode node = objectMapper.valueToTree(result);
        JsonNode content = node.path("content");
        if (content.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode item : content) {
                if (item.path("type").asText("").equals("text")) {
                    String text = item.path("text").asText("");
                    if (!text.isBlank()) parts.add(text);
                }
            }
            if (!parts.isEmpty()) {
                return String.join("\n", parts);
            }
        }
        JsonNode structuredContent = node.path("structuredContent");
        if (structuredContent.isTextual() && !structuredContent.asText().isBlank()) {
            return structuredContent.asText();
        }
        if (structuredContent.isObject() || structuredContent.isArray()) {
            return structuredContent.toPrettyString();
        }
        JsonNode text = node.path("text");
        if (text.isTextual() && !text.asText().isBlank()) {
            return text.asText();
        }
        return node.toPrettyString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private Set<String> parseAllowlist(String value) {
        Set<String> defaults = new LinkedHashSet<>(List.of("understand_image", "web_search"));
        if (isBlank(value)) {
            return defaults;
        }
        Set<String> parsed = new LinkedHashSet<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .forEach(parsed::add);
        return parsed.isEmpty() ? defaults : parsed;
    }

    private static final class McpSessionState {
        private final String protocolVersion;
        private final String sessionId;

        private McpSessionState(String protocolVersion, String sessionId) {
            this.protocolVersion = protocolVersion;
            this.sessionId = sessionId;
        }
    }
}
