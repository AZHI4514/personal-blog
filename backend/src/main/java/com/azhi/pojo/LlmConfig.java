package com.azhi.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LlmConfig {
    private Long id;
    private Long userId;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private String customPrompt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
