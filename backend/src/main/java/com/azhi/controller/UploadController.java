package com.azhi.controller;

import com.azhi.pojo.Result;
import com.azhi.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/images")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(Map.of("filePath", uploadService.uploadImage(file)));
    }
}
