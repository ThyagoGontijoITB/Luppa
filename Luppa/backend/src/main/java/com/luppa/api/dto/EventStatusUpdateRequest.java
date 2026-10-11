package com.luppa.api.dto;

import com.luppa.api.entity.EventStatus;
import jakarta.validation.constraints.NotNull;

public record EventStatusUpdateRequest(
        @NotNull EventStatus status
) {
}
