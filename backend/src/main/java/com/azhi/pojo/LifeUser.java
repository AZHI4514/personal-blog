package com.azhi.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LifeUser {
    private Long id;
    private String deviceId;
    private LocalDateTime createdAt;
}
