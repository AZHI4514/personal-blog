package com.azhi.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class McpConfig {

    @Value("${aliyun.api-key}")
    private String apiKey;

    @Bean
    public McpToolProvider mcpToolProvider() {
        // 1. 构建 HTTP 传输层
        McpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url("https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/mcp")
                // 直接传入一个包含 Authorization 头的 Map（静态头，适用于 apiKey 固定不变）
                .customHeaders(new HashMap<String, String>() {{
                    put("Authorization", "Bearer " + apiKey);
                }})
                .logRequests(true)
                .logResponses(true)
                .build();

        // 2. 创建 MCP 客户端
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        // 3. 创建并返回 MCP 工具提供者
        return McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();
    }
}