package com.azhi.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    private Long id;
    private String path;
    private String author;
    private LocalDateTime createTime;
}
