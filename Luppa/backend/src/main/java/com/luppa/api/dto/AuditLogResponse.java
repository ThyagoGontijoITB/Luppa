package com.luppa.api.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        LocalDateTime operatedAt,
        AuditUserResponse user,
        Integer userId,
        Integer historicalUserId,
        String tableName,
        String operation,
        Integer recordId,
        String oldData,
        String newData
) {
}
