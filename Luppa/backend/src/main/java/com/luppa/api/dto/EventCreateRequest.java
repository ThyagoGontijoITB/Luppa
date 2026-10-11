package com.luppa.api.dto;

import com.luppa.api.entity.EventStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventCreateRequest(
        @Size(max = 40) @NotBlank String title,
        @Size(max = 150) @NotBlank String description,
        @Size(max = 80) @NotBlank String categorySlug,
        @Size(max = 100) String location,
        @Valid @NotNull EventAddressRequest address,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @FutureOrPresent @NotNull LocalDateTime startDateTime,
        @NotNull LocalDateTime endDateTime,
        EventStatus status
) {
}
