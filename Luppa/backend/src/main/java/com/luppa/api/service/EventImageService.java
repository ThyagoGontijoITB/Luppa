package com.luppa.api.service;

import com.luppa.api.dto.EventImageResponse;
import com.luppa.api.entity.Event;
import com.luppa.api.entity.EventImage;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.mapper.EventImageMapper;
import com.luppa.api.repository.EventRepository;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.image.ImageStorage;
import com.luppa.api.service.image.ImageStorageException;
import com.luppa.api.service.image.ImageUploadTarget;
import com.luppa.api.service.image.StoredImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EventImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventImageService.class);

    private final EventRepository eventRepository;
    private final ImageStorage imageStorage;

    public EventImageService(EventRepository eventRepository, ImageStorage imageStorage) {
        this.eventRepository = eventRepository;
        this.imageStorage = imageStorage;
    }

    @Transactional
    public EventImageResponse replace(Integer eventId, MultipartFile file, AuthUser authUser) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        requireOwnerOrAdmin(event, authUser);
        String oldReference = event.getCoverImagePath();

        StoredImage storedImage;
        try {
            storedImage = imageStorage.upload(file, ImageUploadTarget.EVENT_COVER);
        } catch (ImageStorageException exception) {
            throw new InvalidRequestException("Could not store image");
        }

        cleanupOnRollback(storedImage.reference());
        event.setCoverImagePath(storedImage.reference());
        cleanupAfterCommit(oldReference);

        EventImage eventImage = EventImage.fromPath(event.getId(), storedImage.reference());
        eventImage.setFileName(file.getOriginalFilename() == null ? storedImage.reference() : file.getOriginalFilename());
        eventImage.setStorageKey(storedImage.reference());
        eventImage.setUrl(storedImage.url());
        eventImage.setMimeType(storedImage.mimeType());
        eventImage.setFileSize(storedImage.fileSize());
        eventImage.setWidth(storedImage.width());
        eventImage.setHeight(storedImage.height());
        return EventImageMapper.toResponse(eventImage);
    }

    @Transactional
    public void delete(Integer eventId, AuthUser authUser) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        requireOwnerOrAdmin(event, authUser);
        deleteExplicitly(event.getCoverImagePath());
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

    private void cleanupAfterCommit(String reference) {
        if (!imageStorage.isManagedReference(reference)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteCloudinaryImage(reference);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteCloudinaryImage(reference);
            }
        });
    }

    private void cleanupOnRollback(String reference) {
        if (!imageStorage.isManagedReference(reference)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteCloudinaryImage(reference);
                }
            }
        });
    }

    private void deleteCloudinaryImage(String reference) {
        try {
            imageStorage.delete(reference);
        } catch (ImageStorageException exception) {
            LOGGER.warn("Could not delete Cloudinary event image reference={}", reference);
        }
    }

    private void deleteExplicitly(String reference) {
        if (!imageStorage.isManagedReference(reference)) {
            return;
        }
        try {
            imageStorage.delete(reference);
        } catch (ImageStorageException exception) {
            throw new InvalidRequestException("Could not delete image");
        }
    }
}
