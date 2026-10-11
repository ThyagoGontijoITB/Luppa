package com.luppa.api.controller;

import com.luppa.api.dto.AuditLogResponse;
import com.luppa.api.dto.PageResponse;
import com.luppa.api.service.AuditLogService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditLogController {
    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public PageResponse<AuditLogResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 25, sort = "operatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return PageResponse.from(auditLogService.search(search, operation, tableName, user, startDate, endDate, pageable));
    }

    @GetMapping("/tables")
    public List<String> tableNames() {
        return auditLogService.findTableNames();
    }
}
