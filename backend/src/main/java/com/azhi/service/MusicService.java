package com.azhi.service;

import com.azhi.pojo.Music;

import java.util.List;

public interface MusicService {
    List<Music> getAllMusics();
    void createMusic(Music music);
    void deleteMusic(Long musicId);
}
