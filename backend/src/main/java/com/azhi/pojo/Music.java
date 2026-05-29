package com.azhi.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Music {
    private Long id;
    private Long musicId;
    private String title;
    private String artist;
    private String filePath;
    private String coverPath;
    private LocalDateTime updateTime;

    public Long getMusicId() {
        return musicId != null ? musicId : id;
    }

    public void setId(Long id) {
        this.id = id;
        this.musicId = id;
    }

    public void setMusicId(Long musicId) {
        this.musicId = musicId;
        this.id = musicId;
    }
}
