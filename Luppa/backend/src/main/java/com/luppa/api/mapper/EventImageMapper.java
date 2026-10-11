package com.luppa.api.mapper;

import com.luppa.api.config.StorageProperties;
import com.luppa.api.config.CloudinaryProperties;
import com.luppa.api.dto.EventImageResponse;
import com.luppa.api.entity.EventImage;
import org.springframework.stereotype.Component;

@Component
public class EventImageMapper {
    private static StorageProperties storageProperties;
    private static CloudinaryProperties cloudinaryProperties;

    public EventImageMapper(StorageProperties storageProperties, CloudinaryProperties cloudinaryProperties) {
        EventImageMapper.storageProperties = storageProperties;
        EventImageMapper.cloudinaryProperties = cloudinaryProperties;
    }

    public static EventImageResponse toResponse(EventImage image) {
        if (image == null) {
            return null;
        }

        String url = image.getUrl();
        if (url != null && !url.isBlank() && !url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("/")) {
            if (cloudinaryProperties != null && cloudinaryProperties.isConfigured() && cloudinaryProperties.isManagedPublicId(url)) {
                url = cloudinaryProperties.deliveryUrl(url);
            } else {
                url = storageProperties.publicBaseUrl().replaceAll("/$", "") + "/" + url;
            }
        }

        return new EventImageResponse(
                image.getId(),
                image.getFileName(),
                image.getStorageKey(),
                url,
                image.getMimeType(),
                image.getFileSize(),
                image.getWidth(),
                image.getHeight(),
                image.getCreatedAt()
        );
    }
}
