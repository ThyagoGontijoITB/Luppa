package com.luppa.api.service;

import com.luppa.api.dto.UserResponse;
import com.luppa.api.entity.User;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.mapper.UserMapper;
import com.luppa.api.repository.UserRepository;
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
public class ProfileImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileImageService.class);

    private final UserRepository userRepository;
    private final ImageStorage imageStorage;

    public ProfileImageService(UserRepository userRepository, ImageStorage imageStorage) {
        this.userRepository = userRepository;
        this.imageStorage = imageStorage;
    }

    @Transactional
    public UserResponse replace(MultipartFile file, AuthUser authUser) {
        User user = userRepository.findById(authUser.idUsuario()).orElseThrow();
        String oldReference = user.getProfileImagePath();

        StoredImage storedImage;
        try {
            storedImage = imageStorage.upload(file, ImageUploadTarget.PROFILE);
        } catch (ImageStorageException exception) {
            throw new InvalidRequestException("Could not store image");
        }

        cleanupOnRollback(storedImage.reference());
        user.setProfileImagePath(storedImage.reference());
        cleanupAfterCommit(oldReference);
        return UserMapper.toResponse(user);
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
            LOGGER.warn("Could not delete Cloudinary profile image reference={}", reference);
        }
    }
}
