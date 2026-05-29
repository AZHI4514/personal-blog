package com.azhi.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorStat {
    private Long id;
    private String ip;
    private Long total;
    private LocalDateTime updateTime;
}
