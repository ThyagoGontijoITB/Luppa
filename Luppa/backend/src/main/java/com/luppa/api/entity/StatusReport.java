package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "statusDenuncia")
public class StatusReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idStatusDenuncia")
    private Integer id;

    @Column(name = "descricao", nullable = false, length = 50)
    private String description;

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public ReportStatus getStatus() {
        return switch (id) {
            case 0 -> ReportStatus.PENDING;
            case 1 -> ReportStatus.UNDER_REVIEW;
            case 2 -> ReportStatus.RESOLVED;
            case 3 -> ReportStatus.DISMISSED;
            default -> throw new IllegalStateException("Unknown statusDenuncia idStatusDenuncia: " + id);
        };
    }

    public static int idFor(ReportStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case UNDER_REVIEW -> 1;
            case RESOLVED -> 2;
            case DISMISSED -> 3;
        };
    }
}
