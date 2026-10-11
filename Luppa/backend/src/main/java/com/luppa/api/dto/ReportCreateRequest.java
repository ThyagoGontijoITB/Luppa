package com.luppa.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotNull Integer categoryId,
        @NotBlank @Size(max = 350) String description,
        Integer ratingId
) {
}
