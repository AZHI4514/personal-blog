package com.azhi.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LifeEvent {
    private Long id;
    private Long characterId;
    private Integer age;
    private String description;
    private String choiceMade;
    private String effects;
    private LocalDateTime createdAt;
}
