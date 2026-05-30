package com.azhi.controller;

import com.azhi.pojo.Image;
import com.azhi.pojo.Result;
import com.azhi.service.GalleryService;
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
@RequestMapping("/images")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;

    @GetMapping
    public Result<List<Image>> getAllImages() {
        List<Image> images = galleryService.getAllImages();
        return Result.success(images);
    }

    @PostMapping
    public Result<Void> createImage(@RequestBody Image image) {
        galleryService.createImage(image);
        return Result.success();
    }

    @DeleteMapping("/{imageId}")
    public Result<Void> deleteImage(@PathVariable Long imageId) {
        galleryService.deleteImage(imageId);
        return Result.success();
    }
}
