package com.luppa.api.service;

import com.luppa.api.dto.EventImageResponse;
import com.luppa.api.entity.Event;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.repository.EventRepository;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.image.ImageStorage;
import com.luppa.api.service.image.ImageStorageException;
import com.luppa.api.service.image.ImageUploadTarget;
import com.luppa.api.service.image.StoredImage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventImageServiceTest {
    private final EventRepository eventRepository = mock(EventRepository.class);
    private final FakeImageStorage imageStorage = new FakeImageStorage();
    private final EventImageService service = new EventImageService(eventRepository, imageStorage);

    @Test
    void creatorCanReplaceEventCoverWithCloudinaryReference() {
        Event event = event(10, creator(24, UserRole.CREATOR), "luppa/events/old-cover");
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));

        EventImageResponse response = service.replace(10, pngFile(), authUser(24, UserRole.CREATOR));

        assertThat(event.getCoverImagePath()).isEqualTo("luppa/events/new-image");
        assertThat(response.storageKey()).isEqualTo("luppa/events/new-image");
        assertThat(response.url()).isEqualTo("https://res.cloudinary.com/demo/image/upload/luppa/events/new-image");
        assertThat(imageStorage.uploadTargets).containsExactly(ImageUploadTarget.EVENT_COVER);
        assertThat(imageStorage.deleted).containsExactly("luppa/events/old-cover");
    }

    @Test
    void replacingLegacyLocalCoverDoesNotDeleteExistingLocalFile() {
        Event event = event(10, creator(24, UserRole.CREATOR), "legacy-local-file.png");
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));

        service.replace(10, pngFile(), authUser(24, UserRole.CREATOR));

        assertThat(event.getCoverImagePath()).isEqualTo("luppa/events/new-image");
        assertThat(imageStorage.deleted).isEmpty();
    }

    @Test
    void explicitDeleteFailsBeforeClearingReferenceWhenCloudinaryDeleteFails() {
        Event event = event(10, creator(24, UserRole.CREATOR), "luppa/events/current-cover");
        imageStorage.failDelete = true;
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.delete(10, authUser(24, UserRole.CREATOR)))
                .isInstanceOf(InvalidRequestException.class);

        assertThat(event.getCoverImagePath()).isEqualTo("luppa/events/current-cover");
    }

    @Test
    void nonOwnerCreatorCannotReplaceEventCover() {
        Event event = event(10, creator(24, UserRole.CREATOR), "");
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.replace(10, pngFile(), authUser(30, UserRole.CREATOR)))
                .isInstanceOf(ForbiddenOperationException.class);

        assertThat(imageStorage.uploadTargets).isEmpty();
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3});
    }

    private Event event(Integer id, User creator, String coverImagePath) {
        Event event = new Event();
        setId(event, id);
        event.setCreator(creator);
        event.setCoverImagePath(coverImagePath);
        return event;
    }

    private AuthUser authUser(Integer id, UserRole role) {
        return new AuthUser(creator(id, role));
    }

    private User creator(Integer id, UserRole roleName) {
        User user = new User();
        setId(user, id);
        user.setName("User Test");
        user.setEmail("user" + id + "@luppa.com");
        user.setRole(role(roleName));
        user.setPasswordHash("hash");
        user.setEmailVerified(true);
        return user;
    }

    private Role role(UserRole roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
    }

    private void setId(Object target, Integer id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class FakeImageStorage implements ImageStorage {
        private final List<ImageUploadTarget> uploadTargets = new ArrayList<>();
        private final List<String> deleted = new ArrayList<>();
        private boolean failDelete;

        @Override
        public StoredImage upload(MultipartFile file, ImageUploadTarget target) {
            uploadTargets.add(target);
            return new StoredImage(
                    "luppa/events/new-image",
                    "https://res.cloudinary.com/demo/image/upload/luppa/events/new-image",
                    file.getContentType(),
                    file.getSize(),
                    10,
                    10
            );
        }

        @Override
        public void delete(String reference) {
            if (failDelete) {
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
