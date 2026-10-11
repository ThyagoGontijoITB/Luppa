package com.luppa.api.service;

import com.luppa.api.config.StorageProperties;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.entity.UserStatus;
import com.luppa.api.exception.ConflictException;
import com.luppa.api.repository.RoleRepository;
import com.luppa.api.repository.UserDeletionRepository;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceDeletionTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UserDeletionRepository userDeletionRepository = mock(UserDeletionRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @TempDir
    Path tempDir;

    @Test
    void deletesUserWithDependenciesInForeignKeySafeOrder() {
        User target = user(20, UserRole.CREATOR);
        when(userRepository.findById(20)).thenReturn(Optional.of(target));
        when(userDeletionRepository.findEventIdsByCreator(20)).thenReturn(List.of(100, 101));
        when(userDeletionRepository.findEventContactIdsByCreator(20)).thenReturn(List.of(200, 201));
        when(userDeletionRepository.findEventAddressIdsByCreator(20)).thenReturn(List.of(300, 301));
        when(userDeletionRepository.findEventImagePathsByCreator(20)).thenReturn(List.of("event-a.jpg", "event-b.jpg"));

        service().delete(20, authUser(1, UserRole.ADMIN));

        InOrder order = inOrder(userDeletionRepository, userRepository);
        order.verify(userDeletionRepository).findEventIdsByCreator(20);
        order.verify(userDeletionRepository).findEventContactIdsByCreator(20);
        order.verify(userDeletionRepository).findEventAddressIdsByCreator(20);
        order.verify(userDeletionRepository).findEventImagePathsByCreator(20);
        order.verify(userDeletionRepository).deleteReportsForUserCascade(20, List.of(100, 101));
        order.verify(userDeletionRepository).deleteSavedEventsForUserCascade(20, List.of(100, 101));
        order.verify(userDeletionRepository).deleteRatingsForUserCascade(20, List.of(100, 101));
        order.verify(userDeletionRepository).deleteEventSocialLinks(List.of(100, 101));
        order.verify(userDeletionRepository).deleteUserOwnedRecords(20);
        order.verify(userDeletionRepository).deleteEventsByCreator(20);
        order.verify(userDeletionRepository).deleteUnreferencedEventContacts(List.of(200, 201));
        order.verify(userDeletionRepository).deleteUnreferencedEventAddresses(List.of(300, 301));
        order.verify(userRepository).delete(target);
        order.verify(userRepository).flush();
    }

    @Test
    void deletesUserWithoutDependencies() {
        User target = user(20, UserRole.USER);
        when(userRepository.findById(20)).thenReturn(Optional.of(target));
        when(userDeletionRepository.findEventIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventContactIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventAddressIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventImagePathsByCreator(20)).thenReturn(List.of());

        service().delete(20, authUser(1, UserRole.ADMIN));

        verify(userRepository).delete(target);
        verify(userRepository).flush();
    }

    @Test
    void physicalDeletionIsNotBlockedByAuditLogHistory() {
        User target = user(20, UserRole.USER);
        when(userRepository.findById(20)).thenReturn(Optional.of(target));
        when(userDeletionRepository.findEventIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventContactIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventAddressIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventImagePathsByCreator(20)).thenReturn(List.of());

        service().delete(20, authUser(1, UserRole.ADMIN));

        verify(userDeletionRepository).deleteUserOwnedRecords(20);
        verify(userRepository).delete(target);
        verify(userRepository).flush();
    }

    @Test
    void doesNotDeleteUserOrFilesWhenDependentDeletionFails() throws Exception {
        User target = user(20, UserRole.USER);
        target.setProfileImagePath("profile.jpg");
        Path profileFile = tempDir.resolve("profile").resolve("profile.jpg");
        Files.createDirectories(profileFile.getParent());
        Files.writeString(profileFile, "image");

        when(userRepository.findById(20)).thenReturn(Optional.of(target));
        when(userDeletionRepository.findEventIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventContactIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventAddressIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventImagePathsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.deleteReportsForUserCascade(20, List.of()))
                .thenThrow(new DataIntegrityViolationException("fk"));

        assertThatThrownBy(() -> service().delete(20, authUser(1, UserRole.ADMIN)))
                .isInstanceOf(ConflictException.class);

        assertThat(Files.exists(profileFile)).isTrue();
        verify(userRepository, never()).delete(target);
    }

    @Test
    void deletesCollectedImagesOnlyAfterSuccessfulDatabaseWork() throws Exception {
        User target = user(20, UserRole.CREATOR);
        target.setProfileImagePath("profile.jpg");
        Path profileFile = tempDir.resolve("profile").resolve("profile.jpg");
        Path eventFile = tempDir.resolve("events").resolve("event.jpg");
        Files.createDirectories(profileFile.getParent());
        Files.createDirectories(eventFile.getParent());
        Files.writeString(profileFile, "profile");
        Files.writeString(eventFile, "event");

        when(userRepository.findById(20)).thenReturn(Optional.of(target));
        when(userDeletionRepository.findEventIdsByCreator(20)).thenReturn(List.of(100));
        when(userDeletionRepository.findEventContactIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventAddressIdsByCreator(20)).thenReturn(List.of());
        when(userDeletionRepository.findEventImagePathsByCreator(20)).thenReturn(List.of("event.jpg"));

        service().delete(20, authUser(1, UserRole.ADMIN));

        assertThat(Files.exists(profileFile)).isFalse();
        assertThat(Files.exists(eventFile)).isFalse();
    }

    private UserService service() {
        StorageProperties storageProperties = new StorageProperties(
                tempDir.resolve("events").toString(),
                "",
                tempDir.resolve("profile").toString(),
                ""
        );
        return new UserService(userRepository, roleRepository, userDeletionRepository, passwordEncoder, storageProperties);
    }

    private AuthUser authUser(Integer id, UserRole role) {
        return new AuthUser(user(id, role));
    }

    private User user(Integer id, UserRole roleName) {
        User user = new User();
        setId(user, id);
        user.setName("User Test");
        user.setEmail("user" + id + "@luppa.com");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhone("11999999999");
        user.setPasswordHash("hash");
        user.setRole(role(roleName));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
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
}
