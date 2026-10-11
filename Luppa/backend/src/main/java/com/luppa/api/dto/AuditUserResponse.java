package com.luppa.api.dto;

public record AuditUserResponse(
        Integer id,
        String name,
        String email
) {
}
