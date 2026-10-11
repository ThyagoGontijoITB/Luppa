package com.luppa.api.service;

import com.luppa.api.dto.UserCreateRequest;
import com.luppa.api.dto.UserResponse;
import com.luppa.api.dto.UserUpdateRequest;
import com.luppa.api.entity.Role;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.entity.UserStatus;
import com.luppa.api.config.StorageProperties;
import com.luppa.api.exception.ConflictException;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.mapper.UserMapper;
import com.luppa.api.repository.RoleRepository;
import com.luppa.api.repository.UserDeletionRepository;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDeletionRepository userDeletionRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageProperties storageProperties;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserDeletionRepository userDeletionRepository,
            PasswordEncoder passwordEncoder,
            StorageProperties storageProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userDeletionRepository = userDeletionRepository;
        this.passwordEncoder = passwordEncoder;
        this.storageProperties = storageProperties;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String search, UserRole role, UserStatus status, Pageable pageable) {
        Integer roleId = role == null ? null : Role.idFor(role);
        return userRepository.search(blankToNull(search), role, roleId, status, pageable).map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Integer id) {
        return UserMapper.toResponse(find(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request, AuthUser authUser) {
        authorizeCreate(request.role(), authUser);
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already registered");
        }
        User user = new User();
        apply(user, request.name(), request.email(), request.dateOfBirth(), request.phone(), request.role(), request.status());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmailVerified(request.role().isAdminLike());
        if (user.isEmailVerified()) {
            user.setEmailVerifiedAt(java.time.LocalDateTime.now());
        }
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Integer id, UserUpdateRequest request, AuthUser authUser) {
        User user = find(id);
        authorizeUpdate(user, request, authUser);
        userRepository.findByEmailIgnoreCase(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Email already registered");
                });
        apply(user, request.name(), request.email(), request.dateOfBirth(), request.phone(), request.role(), request.status());
        return UserMapper.toResponse(user);
    }

    @Transactional
    public void delete(Integer id, AuthUser authUser) {
        if (authUser != null && id.equals(authUser.idUsuario())) {
            throw new InvalidRequestException("Você não pode excluir sua própria conta administrativa.");
        }
        User user = find(id);
        if (isProtected(user)) {
            throw new ForbiddenOperationException("Contas LUPPA_ADMIN não podem ser excluídas por operações administrativas comuns.");
        }
        if (user.getRole().getName() == UserRole.ADMIN && actorRole(authUser) != UserRole.LUPPA_ADMIN) {
            throw new ForbiddenOperationException("Somente LUPPA_ADMIN pode excluir administradores.");
        }
        List<Integer> eventIds = userDeletionRepository.findEventIdsByCreator(id);
        List<Integer> contactIds = userDeletionRepository.findEventContactIdsByCreator(id);
        List<Integer> addressIds = userDeletionRepository.findEventAddressIdsByCreator(id);
        Set<String> profileImagePaths = imagePathSet(user.getProfileImagePath());
        Set<String> eventImagePaths = imagePathSet(userDeletionRepository.findEventImagePathsByCreator(id));

        try {
            userDeletionRepository.deleteReportsForUserCascade(id, eventIds);
            userDeletionRepository.deleteSavedEventsForUserCascade(id, eventIds);
            userDeletionRepository.deleteRatingsForUserCascade(id, eventIds);
            userDeletionRepository.deleteEventSocialLinks(eventIds);
            userDeletionRepository.deleteUserOwnedRecords(id);
            userDeletionRepository.deleteEventsByCreator(id);
            userDeletionRepository.deleteUnreferencedEventContacts(contactIds);
            userDeletionRepository.deleteUnreferencedEventAddresses(addressIds);
            userRepository.delete(user);
            userRepository.flush();
            registerPostCommitImageCleanup(profileImagePaths, eventImagePaths);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Não é possível excluir este usuário porque existem registros vinculados.");
        }
    }

    private User find(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void authorizeCreate(UserRole requestedRole, AuthUser authUser) {
        UserRole actorRole = actorRole(authUser);
        if (requestedRole == UserRole.LUPPA_ADMIN) {
            throw new ForbiddenOperationException("LUPPA_ADMIN deve ser provisionado manualmente pelo banco de dados.");
        }
        if (requestedRole == UserRole.ADMIN && actorRole != UserRole.LUPPA_ADMIN) {
            throw new ForbiddenOperationException("Somente LUPPA_ADMIN pode criar administradores.");
        }
    }

    private void authorizeUpdate(User target, UserUpdateRequest request, AuthUser authUser) {
        UserRole actorRole = actorRole(authUser);
        UserRole currentRole = target.getRole().getName();
        UserRole requestedRole = request.role();
        boolean self = authUser != null && target.getId().equals(authUser.idUsuario());

        if (currentRole == UserRole.LUPPA_ADMIN) {
            throw new ForbiddenOperationException("Contas LUPPA_ADMIN são protegidas contra alteração por endpoints administrativos.");
        }
        if (requestedRole == UserRole.LUPPA_ADMIN) {
            throw new ForbiddenOperationException("LUPPA_ADMIN deve ser provisionado manualmente pelo banco de dados.");
        }
        if (self && currentRole != requestedRole) {
            throw new ForbiddenOperationException("Você não pode alterar seu próprio papel administrativo.");
        }
        if ((currentRole == UserRole.ADMIN || requestedRole == UserRole.ADMIN) && actorRole != UserRole.LUPPA_ADMIN) {
            throw new ForbiddenOperationException("Somente LUPPA_ADMIN pode promover ou rebaixar administradores.");
        }
    }

    private UserRole actorRole(AuthUser authUser) {
        if (authUser == null) {
            throw new ForbiddenOperationException("Usuário autenticado não identificado.");
        }
        return authUser.user().getRole().getName();
    }

    private boolean isProtected(User user) {
        return user.getRole().getName() == UserRole.LUPPA_ADMIN;
    }

    private Set<String> imagePathSet(String path) {
        Set<String> paths = new LinkedHashSet<>();
        if (path != null && !path.isBlank()) {
            paths.add(path);
        }
        return paths;
    }

    private Set<String> imagePathSet(List<String> paths) {
        Set<String> cleanPaths = new LinkedHashSet<>();
        for (String path : paths) {
            if (path != null && !path.isBlank()) {
                cleanPaths.add(path);
            }
        }
        return cleanPaths;
    }

    private void registerPostCommitImageCleanup(Set<String> profileImagePaths, Set<String> eventImagePaths) {
        if (profileImagePaths.isEmpty() && eventImagePaths.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cleanupImages(profileImagePaths, eventImagePaths);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cleanupImages(profileImagePaths, eventImagePaths);
            }
        });
    }

    private void cleanupImages(Set<String> profileImagePaths, Set<String> eventImagePaths) {
        Path profileStoragePath = Path.of(storageProperties.profileLocalDirectory()).toAbsolutePath().normalize();
        Path eventStoragePath = Path.of(storageProperties.localDirectory()).toAbsolutePath().normalize();
        profileImagePaths.forEach(path -> deleteStoredFile(profileStoragePath, path, "profile"));
        eventImagePaths.forEach(path -> deleteStoredFile(eventStoragePath, path, "event"));
    }

    private void deleteStoredFile(Path storagePath, String storageKey, String imageType) {
        try {
            Path target = storagePath.resolve(storageKey).normalize();
            if (!target.startsWith(storagePath)) {
                LOGGER.warn("Skipped {} image cleanup because the stored key resolved outside the configured directory.", imageType);
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            LOGGER.warn("Failed to clean up {} image after user deletion.", imageType, exception);
        }
    }

    private void apply(User user, String name, String email, java.time.LocalDate dateOfBirth, String phone, UserRole roleName, UserStatus status) {
        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        user.setName(name);
        user.setEmail(email.toLowerCase());
        user.setDateOfBirth(dateOfBirth);
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(status);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
