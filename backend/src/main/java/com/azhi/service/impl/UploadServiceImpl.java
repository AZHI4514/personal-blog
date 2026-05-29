package com.azhi.service.impl;

import com.azhi.service.UploadService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Path IMAGE_UPLOAD_DIR = Paths.get("uploads", "images");

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file cannot be empty");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only jpg, jpeg, png, gif and webp images are supported");
        }

        try {
            Files.createDirectories(IMAGE_UPLOAD_DIR);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = IMAGE_UPLOAD_DIR.resolve(filename);
            file.transferTo(target);
            return "/uploads/images/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Image save failed", ex);
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
