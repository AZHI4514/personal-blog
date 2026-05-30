package com.azhi.service.impl;

import com.azhi.mapper.MusicMapper;
import com.azhi.pojo.Music;
import com.azhi.service.MusicService;
import com.azhi.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MusicServiceImpl implements MusicService {

    private final MusicMapper musicMapper;
    private final UploadService uploadService;

    @Override
    public List<Music> getAllMusics() {
        return musicMapper.findAllMusics();
    }

    @Override
    public void createMusic(Music music) {
        musicMapper.insertMusic(music);
    }

    @Override
    public void deleteMusic(Long musicId) {
        Music music = musicMapper.findMusicById(musicId);
        if (music == null) {
            throw new IllegalArgumentException("Music not found");
        }
        musicMapper.deleteMusicById(musicId);
        uploadService.deleteStoredFile(music.getFilePath());
        uploadService.deleteStoredFile(music.getCoverPath());
    }
}
