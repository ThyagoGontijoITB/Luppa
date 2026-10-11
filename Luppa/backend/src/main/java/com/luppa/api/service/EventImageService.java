package com.luppa.api.service;

import com.luppa.api.config.StorageProperties;
import com.luppa.api.dto.EventImageResponse;
import com.luppa.api.entity.Event;
import com.luppa.api.entity.EventImage;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.mapper.EventImageMapper;
import com.luppa.api.repository.EventRepository;
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
public class EventImageService {
    private static final long MAX_SIZE = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final EventRepository eventRepository;
    private final StorageProperties storageProperties;
    private Path storagePath;

    public EventImageService(EventRepository eventRepository, StorageProperties storageProperties) {
        this.eventRepository = eventRepository;
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    void init() throws IOException {
        storagePath = Path.of(storageProperties.localDirectory()).toAbsolutePath().normalize();
        Files.createDirectories(storagePath);
    }

    @Transactional
    public EventImageResponse replace(Integer eventId, MultipartFile file, AuthUser authUser) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        requireOwnerOrAdmin(event, authUser);
        validate(file);

        if (event.getCoverImagePath() != null && !event.getCoverImagePath().isBlank()) {
            deleteFile(event.getCoverImagePath());
        }

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
                throw new InvalidRequestException("Invalid image file");
            }

            event.setCoverImagePath(storageKey);

            EventImage eventImage = EventImage.fromPath(event.getId(), storageKey);
            eventImage.setFileName(file.getOriginalFilename() == null ? storageKey : file.getOriginalFilename());
            eventImage.setStorageKey(storageKey);
            eventImage.setUrl(storageProperties.publicBaseUrl() + "/" + storageKey);
            eventImage.setMimeType(file.getContentType());
            eventImage.setFileSize(file.getSize());
            if (image != null) {
                eventImage.setWidth(image.getWidth());
                eventImage.setHeight(image.getHeight());
            }
            return EventImageMapper.toResponse(eventImage);
        } catch (IOException exception) {
            throw new InvalidRequestException("Could not store image");
        }
    }

    @Transactional
    public void delete(Integer eventId, AuthUser authUser) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        requireOwnerOrAdmin(event, authUser);
        deleteFile(event.getCoverImagePath());
        event.setCoverImagePath("");
    }

    private void requireOwnerOrAdmin(Event event, AuthUser authUser) {
        if (authUser.user().getRole().getName().isAdminLike()) {
            return;
        }
        if (!event.getCreator().getId().equals(authUser.idUsuario())) {
            throw new ForbiddenOperationException("Creators can only manage images for their own events");
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
            // Database consistency is more important than failing deletion because of a missing local file.
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
