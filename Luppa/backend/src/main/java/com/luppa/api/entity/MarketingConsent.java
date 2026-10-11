package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "consentimentoMarketingUsuario")
public class MarketingConsent {
    @Id
    @Column(name = "idUsuario")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "idUsuario", nullable = false)
    private User user;

    @Column(name = "aceitouMarketing", nullable = false)
    private boolean acceptedMarketing;

    @Column(name = "dataConsentimento")
    private LocalDateTime consentedAt;

    @Column(name = "dataRevogacao")
    private LocalDateTime revokedAt;

    @Column(name = "dataAtualizacao", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
        if (acceptedMarketing) {
            if (consentedAt == null) {
                consentedAt = updatedAt;
            }
            revokedAt = null;
        } else if (consentedAt != null && revokedAt == null) {
            revokedAt = updatedAt;
        }
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) {
        this.user = user;
    }
    public boolean isAcceptedMarketing() { return acceptedMarketing; }
    public void setAcceptedMarketing(boolean acceptedMarketing) { this.acceptedMarketing = acceptedMarketing; }
    public LocalDateTime getConsentedAt() { return consentedAt; }
    public void setConsentedAt(LocalDateTime consentedAt) { this.consentedAt = consentedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
