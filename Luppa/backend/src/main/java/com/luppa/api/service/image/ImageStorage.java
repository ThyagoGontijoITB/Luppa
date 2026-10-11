package com.luppa.api.service.image;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorage {
    StoredImage upload(MultipartFile file, ImageUploadTarget target);

    void delete(String reference);

    boolean isManagedReference(String reference);
}
