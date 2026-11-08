package com.furniture.rental.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FurnitureImageStorageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final String PUBLIC_PREFIX = "/uploads/furniture/";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final Path uploadRoot;

    public FurnitureImageStorageService(@Value("${app.upload-dir:uploads/furniture}") String uploadDirectory) {
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile upload) throws IOException {
        if (upload == null || upload.isEmpty()) {
            throw new IllegalArgumentException("Choose a JPG or PNG image to upload.");
        }
        if (upload.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Each image must be 5 MB or smaller.");
        }

        String extension = StringUtils.getFilenameExtension(upload.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPG and PNG images are supported.");
        }
        try (var input = upload.getInputStream()) {
            if (ImageIO.read(input) == null) {
                throw new IllegalArgumentException("The uploaded file is not a valid image.");
            }
        }

        Files.createDirectories(uploadRoot);
        String filename = UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
        Path destination = uploadRoot.resolve(filename).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid image filename.");
        }
        try (var input = upload.getInputStream()) {
            Files.copy(input, destination);
        }
        return PUBLIC_PREFIX + filename;
    }

    public void deleteManagedImage(String imageUrl) throws IOException {
        if (imageUrl == null || !imageUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        String filename = imageUrl.substring(PUBLIC_PREFIX.length());
        Path image = uploadRoot.resolve(filename).normalize();
        if (image.startsWith(uploadRoot)) {
            Files.deleteIfExists(image);
        }
    }
}
