package com.azhi.service.impl;

import com.azhi.mapper.ImageMapper;
import com.azhi.pojo.Image;
import com.azhi.service.GalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GalleryServiceImpl implements GalleryService {

    private final ImageMapper imageMapper;

    @Override
    public List<Image> getAllImages() {
        return imageMapper.findAllImages();
    }

    @Override
    public void createImage(Image image) {
        imageMapper.insertImage(image);
    }
}
