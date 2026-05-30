package com.azhi.controller;

import com.azhi.pojo.Music;
import com.azhi.pojo.Result;
import com.azhi.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/musics")
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;

    @GetMapping
    public Result<List<Music>> getAllMusics() {
        List<Music> musics = musicService.getAllMusics();
        return Result.success(musics);
    }

    @PostMapping
    public Result<Void> createMusic(@RequestBody Music music) {
        musicService.createMusic(music);
        return Result.success();
    }

    @DeleteMapping("/{musicId}")
    public Result<Void> deleteMusic(@PathVariable Long musicId) {
        musicService.deleteMusic(musicId);
        return Result.success();
    }
}
