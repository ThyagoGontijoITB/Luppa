package com.luppa.api.service;

import com.luppa.api.dto.AuditLogResponse;
import com.luppa.api.mapper.AuditLogMapper;
import com.luppa.api.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            String search,
            String operation,
            String tableName,
            String user,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        String normalizedSearch = blankToNull(search);
        String normalizedOperation = blankToNull(operation);
        String normalizedTableName = blankToNull(tableName);
        String normalizedUser = blankToNull(user);
        LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endAt = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        return auditLogRepository.search(
                normalizedSearch,
                parseInteger(normalizedSearch),
                parseLong(normalizedSearch),
                normalizedOperation,
                normalizedTableName,
                normalizedUser,
                parseInteger(normalizedUser),
                startAt,
                endAt,
                pageable
        ).map(AuditLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<String> findTableNames() {
        return auditLogRepository.findDistinctTableNames();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() || "all".equalsIgnoreCase(value) ? null : value.trim();
    }

    private Integer parseInteger(String value) {
        Long parsed = parseLong(value);
        if (parsed == null || parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE) {
            return null;
        }
        return parsed.intValue();
    }

    private Long parseLong(String value) {
        if (value == null || !value.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
