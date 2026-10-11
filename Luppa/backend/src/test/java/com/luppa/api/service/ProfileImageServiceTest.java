package com.luppa.api.service;

import com.luppa.api.config.CloudinaryProperties;
import com.luppa.api.config.StorageProperties;
import com.luppa.api.dto.UserResponse;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.mapper.UserMapper;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.image.ImageStorage;
import com.luppa.api.service.image.ImageStorageException;
import com.luppa.api.service.image.ImageUploadTarget;
import com.luppa.api.service.image.StoredImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileImageServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final FakeImageStorage imageStorage = new FakeImageStorage();
    private final ProfileImageService service = new ProfileImageService(userRepository, imageStorage);

    @BeforeEach
    void setUpMapper() {
        new UserMapper(
                new StorageProperties("uploads/events", "http://localhost:8081/uploads/events", "uploads/profile", "http://localhost:8081/uploads/profile"),
                new CloudinaryProperties("demo", "key", "secret", "luppa")
        );
    }

    @Test
    void replaceProfileImageStoresCloudinaryPublicIdAndReturnsCompatibleUrl() {
        User user = user(24, "luppa/profiles/old-profile");
        when(userRepository.findById(24)).thenReturn(Optional.of(user));

        UserResponse response = service.replace(pngFile(), new AuthUser(user));

        assertThat(user.getProfileImagePath()).isEqualTo("luppa/profiles/new-profile");
        assertThat(response.profileImagePath()).isEqualTo("luppa/profiles/new-profile");
        assertThat(response.profileImageUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/luppa/profiles/new-profile");
        assertThat(imageStorage.uploadTargets).containsExactly(ImageUploadTarget.PROFILE);
        assertThat(imageStorage.deleted).containsExactly("luppa/profiles/old-profile");
    }

    @Test
    void replacingLegacyLocalProfileImageDoesNotDeleteExistingLocalFile() {
        User user = user(24, "legacy-profile.png");
        when(userRepository.findById(24)).thenReturn(Optional.of(user));

        service.replace(pngFile(), new AuthUser(user));

        assertThat(user.getProfileImagePath()).isEqualTo("luppa/profiles/new-profile");
        assertThat(imageStorage.deleted).isEmpty();
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "profile.png", "image/png", new byte[]{1, 2, 3});
    }

    private User user(Integer id, String profileImagePath) {
        User user = new User();
        setId(user, id);
        user.setName("User Test");
        user.setEmail("user" + id + "@luppa.com");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhone("11999999999");
        user.setPasswordHash("hash");
        user.setRole(role(UserRole.CREATOR));
        user.setEmailVerified(true);
        user.setProfileImagePath(profileImagePath);
        return user;
    }

    private Role role(UserRole roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
    }

    private void setId(User user, Integer id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class FakeImageStorage implements ImageStorage {
        private final List<ImageUploadTarget> uploadTargets = new ArrayList<>();
        private final List<String> deleted = new ArrayList<>();

        @Override
        public StoredImage upload(MultipartFile file, ImageUploadTarget target) {
            uploadTargets.add(target);
            return new StoredImage(
                    "luppa/profiles/new-profile",
                    "https://res.cloudinary.com/demo/image/upload/luppa/profiles/new-profile",
                    file.getContentType(),
                    file.getSize(),
                    10,
                    10
            );
        }

        @Override
        public void delete(String reference) {
            if ("luppa/profiles/delete-fails".equals(reference)) {
                throw new ImageStorageException("delete failed");
            }
            if (isManagedReference(reference)) {
                deleted.add(reference);
            }
        }

        @Override
        public boolean isManagedReference(String reference) {
            return reference != null && reference.startsWith("luppa/");
        }
    }
}
