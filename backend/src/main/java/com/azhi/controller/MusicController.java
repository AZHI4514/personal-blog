package com.azhi.controller;

import com.azhi.pojo.Music;
import com.azhi.pojo.Result;
import com.azhi.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
