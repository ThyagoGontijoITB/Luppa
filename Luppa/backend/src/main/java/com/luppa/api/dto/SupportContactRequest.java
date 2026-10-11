package com.luppa.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportContactRequest(
        @Size(max = 120) @NotBlank String name,
        @Size(max = 120) @Email @NotBlank String email,
        @Size(max = 80) @NotBlank String category,
        @Size(max = 160) @NotBlank String subject,
        @Size(max = 4000) @NotBlank String message
) {
}
