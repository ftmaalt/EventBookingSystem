package com.bookngo.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
        validateImage(file);
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
    private void validateImage(MultipartFile file) {
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("No file was uploaded.");
            }
            if (file.getSize() > MAX_SIZE_BYTES) {
                throw new IllegalArgumentException("File must be smaller than 5MB.");
            }
            if (!ALLOWED_TYPES.contains(file.getContentType())) {
                throw new IllegalArgumentException("File must be a valid image.");
            }

            String extension = getExtension(file.getOriginalFilename());
            if (!ALLOWED_TYPES.contains(extension)) {
                throw new IllegalArgumentException("Unsupported file type.");
            }

            try {
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image == null) {
                    throw new IllegalArgumentException("File is not a valid image.");
                }
            }catch (IOException e) {
                throw new IllegalArgumentException("Failed to read file.", e);
            }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) return "";
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    public void deleteProfilePicture(String imagePath) {
        if (imagePath == null ||  imagePath.isBlank()) {
            return;
        }
        if (!imagePath.startsWith("/uploads/profile-pictures")) {
            throw new IllegalArgumentException("Invalid path for upload profile picture.");
        }
        try{
            Path uploadpath = Paths.get(uploadDir, "profile-pictures").toAbsolutePath().normalize();
            String fileName= imagePath.substring("/uploads/profile-pictures".length());
            Path targetPath = uploadpath.resolve(fileName).normalize();
            if (!targetPath.startsWith(uploadpath)) {
                throw new IllegalArgumentException("Invalid path for upload profile picture.");
            }
            if (Files.exists(targetPath)) {
                Files.delete(targetPath);
                logger.info("Old profile picture deleted: {}", fileName);
            }
        }catch (IOException e) {
            logger.error("Failed to delete old profile picture", e);
            throw new RuntimeException("Failed to delete old profile picture.", e);
        }
    }
}