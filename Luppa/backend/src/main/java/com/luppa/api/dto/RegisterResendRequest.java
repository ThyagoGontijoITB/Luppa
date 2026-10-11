package com.luppa.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterResendRequest(@Email @NotBlank String email) {
}
