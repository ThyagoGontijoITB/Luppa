package com.luppa.api.entity;

public enum UserRole {
    USER,
    CREATOR,
    ADMIN,
    LUPPA_ADMIN;

    public boolean isAdminLike() {
        return this == ADMIN || this == LUPPA_ADMIN;
    }

    public boolean isPrivileged() {
        return isAdminLike();
    }
}
