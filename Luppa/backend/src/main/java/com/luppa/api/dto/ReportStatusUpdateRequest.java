package com.luppa.api.dto;

import com.luppa.api.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportStatusUpdateRequest(
        @NotNull ReportStatus status,
        @Size(max = 1000) String responseMessage
) {
}
