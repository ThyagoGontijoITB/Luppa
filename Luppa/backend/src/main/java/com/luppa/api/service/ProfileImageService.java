package com.luppa.api.service;

import com.luppa.api.config.StorageProperties;
import com.luppa.api.dto.UserResponse;
import com.luppa.api.entity.User;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.mapper.UserMapper;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.security.AuthUser;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileImageService {
    private static final long MAX_SIZE = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;
    private final StorageProperties storageProperties;
    private Path storagePath;

    public ProfileImageService(UserRepository userRepository, StorageProperties storageProperties) {
        this.userRepository = userRepository;
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    void init() throws IOException {
        storagePath = Path.of(storageProperties.profileLocalDirectory()).toAbsolutePath().normalize();
        Files.createDirectories(storagePath);
    }

    @Transactional
    public UserResponse replace(MultipartFile file, AuthUser authUser) {
        validate(file);
        User user = userRepository.findById(authUser.idUsuario()).orElseThrow();

        String extension = extension(file.getContentType());
        String storageKey = UUID.randomUUID() + extension;
        Path target = storagePath.resolve(storageKey).normalize();
        if (!target.startsWith(storagePath)) {
            throw new InvalidRequestException("Invalid image path");
        }

        try {
            file.transferTo(target);
            BufferedImage image = ImageIO.read(target.toFile());
            if (image == null && !"image/webp".equals(file.getContentType())) {
                Files.deleteIfExists(target);
                throw new InvalidRequestException("Invalid image file");
            }

            deleteFile(user.getProfileImagePath());
            user.setProfileImagePath(storageKey);
            return UserMapper.toResponse(user);
        } catch (IOException exception) {
            throw new InvalidRequestException("Could not store image");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Image file is required");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new InvalidRequestException("Image must be at most 2 MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new InvalidRequestException("Only JPEG, PNG and WebP images are allowed");
        }
    }

    private void deleteFile(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            Path target = storagePath.resolve(storageKey).normalize();
            if (target.startsWith(storagePath)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
            // Keep the persisted profile update even if cleanup of an old local file fails.
        }
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}
