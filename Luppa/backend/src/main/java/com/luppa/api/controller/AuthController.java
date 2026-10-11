package com.luppa.api.controller;

import com.luppa.api.dto.LoginRequest;
import com.luppa.api.dto.LoginResponse;
import com.luppa.api.dto.ForgotPasswordRequest;
import com.luppa.api.dto.ProfileUpdateRequest;
import com.luppa.api.dto.RegisterCompleteResponse;
import com.luppa.api.dto.RegisterRequest;
import com.luppa.api.dto.RegisterResendRequest;
import com.luppa.api.dto.RegisterStartResponse;
import com.luppa.api.dto.RegisterVerifyRequest;
import com.luppa.api.dto.ResendVerificationRequest;
import com.luppa.api.dto.ResetPasswordRequest;
import com.luppa.api.dto.UserResponse;
import com.luppa.api.security.AuthUser;
import com.luppa.api.security.PublicEndpointRateLimiter;
import com.luppa.api.service.AccountTokenService;
import com.luppa.api.service.AuthService;
import com.luppa.api.service.PendingRegistrationService;
import com.luppa.api.service.ProfileImageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final ProfileImageService profileImageService;
    private final AccountTokenService accountTokenService;
    private final PendingRegistrationService pendingRegistrationService;
    private final PublicEndpointRateLimiter rateLimiter;

    public AuthController(
            AuthService authService,
            ProfileImageService profileImageService,
            AccountTokenService accountTokenService,
            PendingRegistrationService pendingRegistrationService,
            PublicEndpointRateLimiter rateLimiter
    ) {
        this.authService = authService;
        this.profileImageService = profileImageService;
        this.accountTokenService = accountTokenService;
        this.pendingRegistrationService = pendingRegistrationService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public RegisterStartResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        rateLimiter.checkVerificationRequest(servletRequest, request.email());
        return pendingRegistrationService.start(request);
    }

    @PostMapping("/register/verify")
    public RegisterCompleteResponse verifyRegistration(@Valid @RequestBody RegisterVerifyRequest request, HttpServletRequest servletRequest) {
        rateLimiter.checkVerificationAttempt(servletRequest, request.email());
        return pendingRegistrationService.verify(request);
    }

    @PostMapping("/register/resend")
    public RegisterStartResponse resendRegistrationCode(@Valid @RequestBody RegisterResendRequest request, HttpServletRequest servletRequest) {
        rateLimiter.checkVerificationRequest(servletRequest, request.email());
        return pendingRegistrationService.resend(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        rateLimiter.checkLogin(servletRequest, request.email());
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        rateLimiter.checkPasswordRecovery(servletRequest, request.email());
        accountTokenService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest servletRequest) {
        rateLimiter.checkPasswordReset(servletRequest);
        accountTokenService.resetPassword(request);
    }

    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam String token, HttpServletRequest servletRequest) {
        rateLimiter.checkVerificationAttempt(servletRequest, "");
        accountTokenService.verifyEmail(token);
    }

    @PostMapping("/resend-verification")
    public void resendVerification(@Valid @RequestBody ResendVerificationRequest request, HttpServletRequest servletRequest) {
        rateLimiter.checkVerificationRequest(servletRequest, request.email());
        accountTokenService.resendVerification(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser authUser) {
        return authService.me(authUser);
    }

    @PutMapping("/me")
    public LoginResponse updateMe(@Valid @RequestBody ProfileUpdateRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return authService.updateMe(request, authUser);
    }

    @PostMapping("/me/image")
    public UserResponse uploadProfileImage(@RequestPart("file") MultipartFile file, @AuthenticationPrincipal AuthUser authUser) {
        return profileImageService.replace(file, authUser);
    }
}
