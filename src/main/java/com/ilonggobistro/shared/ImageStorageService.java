package com.ilonggobistro.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path uploadRoot;

    public ImageStorageService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.containsKey(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPG, PNG, and WEBP image files are allowed.");
        }
        String extension = ALLOWED_CONTENT_TYPES.get(contentType.toLowerCase());
        String safeFileName = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(uploadRoot);
            Path destination = uploadRoot.resolve(safeFileName).normalize();
            if (!destination.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("Invalid upload destination.");
            }
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + safeFileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save uploaded image.", ex);
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }
}
