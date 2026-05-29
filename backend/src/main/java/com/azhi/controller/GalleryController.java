package com.azhi.controller;

import com.azhi.pojo.Image;
import com.azhi.pojo.Result;
import com.azhi.service.GalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;

    @GetMapping
    public Result<List<Image>> getAllImages() {
        List<Image> images = galleryService.getAllImages();
        return Result.success(images);
    }
}
