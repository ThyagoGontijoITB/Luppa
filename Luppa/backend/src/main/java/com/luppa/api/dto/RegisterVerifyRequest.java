package com.luppa.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterVerifyRequest(
        @Email @NotBlank String email,
        @Pattern(regexp = "^\\d{6}$", message = "code must contain exactly 6 digits") String code
) {
}
