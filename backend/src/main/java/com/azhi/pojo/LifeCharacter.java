package com.azhi.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LifeCharacter {
    private Long id;
    private Long userId;
    private String name;
    private Integer age;
    private Integer money;
    private Integer health;
    private Integer happiness;
    private Integer morality;
    private Integer knowledge;
    private Boolean isAlive;
    private Integer generation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
