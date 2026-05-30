package com.azhi.service.impl;

import com.azhi.mapper.ImageMapper;
import com.azhi.pojo.Image;
import com.azhi.service.GalleryService;
import com.azhi.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GalleryServiceImpl implements GalleryService {

    private final ImageMapper imageMapper;
    private final UploadService uploadService;

    @Override
    public List<Image> getAllImages() {
        return imageMapper.findAllImages();
    }

    @Override
    public void createImage(Image image) {
        imageMapper.insertImage(image);
    }

    @Override
    public void deleteImage(Long imageId) {
        Image image = imageMapper.findImageById(imageId);
        if (image == null) {
            throw new IllegalArgumentException("Image not found");
        }
        imageMapper.deleteImageById(imageId);
        uploadService.deleteStoredFile(image.getPath());
    }
}
