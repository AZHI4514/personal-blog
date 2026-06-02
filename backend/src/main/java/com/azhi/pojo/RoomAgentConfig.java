package com.azhi.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomAgentConfig {
    private Long id;
    private String configKey;
    private String apiUrl;
    private String apiKey;
    private String model;
    private String visionMode;
    private String updatedBy;
    private LocalDateTime updateTime;
}
