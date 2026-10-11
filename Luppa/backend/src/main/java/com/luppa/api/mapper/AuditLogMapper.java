package com.luppa.api.mapper;

import com.luppa.api.dto.AuditLogResponse;
import com.luppa.api.dto.AuditUserResponse;
import com.luppa.api.entity.AuditLog;
import com.luppa.api.entity.User;

public final class AuditLogMapper {
    private AuditLogMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog auditLog) {
        User user = auditLog.getUser();
        AuditUserResponse userResponse = user == null
                ? null
                : new AuditUserResponse(user.getId(), user.getName(), user.getEmail());
        Integer historicalUserId = auditLog.getHistoricalUserId();
        Integer displayUserId = historicalUserId == null ? auditLog.getUserId() : historicalUserId;

        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getOperatedAt(),
                userResponse,
                displayUserId,
                historicalUserId,
                auditLog.getTableName(),
                auditLog.getOperation(),
                auditLog.getRecordId(),
                auditLog.getOldData(),
                auditLog.getNewData()
        );
    }
}
