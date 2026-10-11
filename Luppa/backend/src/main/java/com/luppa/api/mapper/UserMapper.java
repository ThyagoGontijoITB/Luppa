package com.luppa.api.mapper;

import com.luppa.api.dto.UserResponse;
import com.luppa.api.config.StorageProperties;
import com.luppa.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    private static StorageProperties storageProperties;

    public UserMapper(StorageProperties storageProperties) {
        UserMapper.storageProperties = storageProperties;
    }

    public static UserResponse toResponse(User user) {
        String profileImagePath = user.getProfileImagePath();
        return new UserResponse(
                user.getId().longValue(),
                user.getName(),
                user.getEmail(),
                user.getDateOfBirth(),
                user.getPhone(),
                toProfileImageUrl(profileImagePath),
                profileImagePath,
                user.getRole().getName(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static String toProfileImageUrl(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/")) {
            return path;
        }
        return storageProperties.profilePublicBaseUrl().replaceAll("/$", "") + "/" + path;
    }
}
