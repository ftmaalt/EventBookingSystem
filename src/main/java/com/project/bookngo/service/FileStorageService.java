package com.project.bookngo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    public String storeProfilePicture(MultipartFile file) {
        logger.info("Profile picture upload requested");
        if (file == null || file.isEmpty()) {
            logger.warn("Profile picture upload failed: no file provided");
            throw new IllegalArgumentException("No file was uploaded.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            logger.warn("Profile picture upload rejected: unsupported file type {}", file.getContentType());
            throw new IllegalArgumentException("Only JPEG, PNG, or WEBP images are allowed.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            logger.warn("Profile picture upload rejected: file size {} bytes exceeds limit", file.getSize());
            throw new IllegalArgumentException("File must be smaller than 5MB.");
        }

        try {
            Path uploadPath = Paths.get(uploadDir, "profile-pictures");
            Files.createDirectories(uploadPath);

            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path targetPath = uploadPath.resolve(filename);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Profile picture stored successfully: {}", filename);

            return "/uploads/profile-pictures/" + filename;
        } catch (IOException e) {
            logger.error("Failed to store profile picture", e);
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) return "";
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}