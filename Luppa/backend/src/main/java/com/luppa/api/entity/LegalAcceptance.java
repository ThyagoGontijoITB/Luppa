package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "aceiteLegalUsuario")
public class LegalAcceptance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAceiteLegal")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private User user;

    @Convert(converter = LegalDocumentTypeConverter.class)
    @Column(name = "tipoDocumento", nullable = false, length = 30)
    private LegalDocumentType documentType;

    @Column(name = "versaoDocumento", nullable = false, length = 30)
    private String documentVersion;

    @Column(name = "dataAceite", nullable = false)
    private LocalDateTime acceptedAt;

    @PrePersist
    void prePersist() {
        if (acceptedAt == null) {
            acceptedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LegalDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(LegalDocumentType documentType) { this.documentType = documentType; }
    public String getDocumentVersion() { return documentVersion; }
    public void setDocumentVersion(String documentVersion) { this.documentVersion = documentVersion; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
}
