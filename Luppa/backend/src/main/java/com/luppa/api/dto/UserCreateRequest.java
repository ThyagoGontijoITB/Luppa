package com.luppa.api.dto;

import com.luppa.api.entity.UserRole;
import com.luppa.api.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserCreateRequest(
        @Size(max = 85) @NotBlank String name,
        @Size(max = 60) @Email @NotBlank String email,
        @Past @NotNull LocalDate dateOfBirth,
        @Pattern(regexp = "^(?:\\d{10,11}|\\(\\d{2}\\) \\d{4,5}-\\d{4})$") @NotBlank String phone,
        @Size(min = 8, max = 128) @NotBlank String password,
        @NotNull UserRole role,
        @NotNull UserStatus status
) {
}
