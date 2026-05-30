package com.azhi.service.impl;

import com.azhi.mapper.MusicMapper;
import com.azhi.pojo.Music;
import com.azhi.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MusicServiceImpl implements MusicService {

    private final MusicMapper musicMapper;

    @Override
    public List<Music> getAllMusics() {
        return musicMapper.findAllMusics();
    }

    @Override
    public void createMusic(Music music) {
        musicMapper.insertMusic(music);
    }
}
