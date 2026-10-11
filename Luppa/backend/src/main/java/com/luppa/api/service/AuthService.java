package com.luppa.api.service;

import com.luppa.api.dto.LoginRequest;
import com.luppa.api.dto.LoginResponse;
import com.luppa.api.dto.ProfileUpdateRequest;
import com.luppa.api.dto.UserResponse;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ConflictException;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.mapper.UserMapper;
import com.luppa.api.repository.UserRepository;
import com.luppa.api.security.AuthUser;
import com.luppa.api.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (DisabledException exception) {
            throw new ForbiddenOperationException("Confirme seu e-mail antes de acessar sua conta.");
        } catch (AuthenticationException exception) {
            throw new InvalidRequestException("Email ou senha inválidos.");
        }
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (requiresEmailVerification(user)) {
            throw new ForbiddenOperationException("Confirme seu e-mail antes de acessar sua conta.");
        }
        AuthUser authUser = new AuthUser(user);
        return new LoginResponse(jwtService.generateToken(authUser), UserMapper.toResponse(user), false);
    }

    public UserResponse me(AuthUser authUser) {
        return UserMapper.toResponse(authUser.user());
    }

    @Transactional
    public LoginResponse updateMe(ProfileUpdateRequest request, AuthUser authUser) {
        User user = userRepository.findById(authUser.idUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.findByEmailIgnoreCase(request.email())
                .filter(existing -> !existing.getId().equals(authUser.idUsuario()))
                .ifPresent(existing -> {
                    throw new ConflictException("Email already registered");
                });

        user.setName(request.name());
        user.setEmail(request.email().toLowerCase());
        user.setDateOfBirth(request.dateOfBirth());
        user.setPhone(request.phone());
        AuthUser updatedAuthUser = new AuthUser(user);
        return new LoginResponse(jwtService.generateToken(updatedAuthUser), UserMapper.toResponse(user), false);
    }

    private boolean requiresEmailVerification(User user) {
        return !user.getRole().getName().isAdminLike() && !user.isEmailVerified();
    }
}
