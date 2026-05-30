package com.azhi.service.impl;

import com.azhi.service.UploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_MUSIC_EXTENSIONS = Set.of("mp3", "wav", "ogg", "flac", "m4a");

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public String uploadImage(MultipartFile file) {
        return uploadFile(file, "images", ALLOWED_IMAGE_EXTENSIONS, "Only jpg, jpeg, png, gif and webp images are supported");
    }

    @Override
    public String uploadMusic(MultipartFile file) {
        return uploadFile(file, "musics", ALLOWED_MUSIC_EXTENSIONS, "Only mp3, wav, ogg, flac and m4a audio files are supported");
    }

    @Override
    public void deleteStoredFile(String filePath) {
        if (filePath == null || filePath.isBlank() || !filePath.startsWith("/uploads/")) {
            return;
        }

        try {
            String relativePath = filePath.substring("/uploads/".length()).replace("/", java.io.File.separator);
            Path target = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(relativePath).normalize();
            Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
            if (!target.startsWith(uploadRoot)) {
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new IllegalStateException("File delete failed", ex);
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String uploadFile(MultipartFile file, String directory, Set<String> allowedExtensions, String errorMessage) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file cannot be empty");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(errorMessage);
        }

        try {
            Path uploadDir = Paths.get(uploadPath, directory).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = uploadDir.resolve(filename);
            file.transferTo(target);
            return "/uploads/" + directory + "/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("File save failed", ex);
        }
    }
}
