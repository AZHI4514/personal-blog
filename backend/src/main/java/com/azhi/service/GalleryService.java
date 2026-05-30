package com.azhi.service;

import com.azhi.pojo.Image;

import java.util.List;

public interface GalleryService {
    List<Image> getAllImages();
    void createImage(Image image);
    void deleteImage(Long imageId);
}
