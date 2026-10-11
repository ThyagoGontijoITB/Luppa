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
@Table(name = "preferenciaComunicacaoUsuario")
public class CommunicationPreference {
    @Id
    @Column(name = "idUsuario")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "idUsuario", nullable = false)
    private User user;

    @Column(name = "receberNovidadesLuppa", nullable = false)
    private boolean receiveLuppaNews;

    @Column(name = "receberRecomendacoesEvento", nullable = false)
    private boolean receiveEventRecommendations;

    @Column(name = "receberLembretesEventoSalvo", nullable = false)
    private boolean receiveSavedEventReminders;

    @Column(name = "dataAtualizacao", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) {
        this.user = user;
    }
    public boolean isReceiveLuppaNews() { return receiveLuppaNews; }
    public void setReceiveLuppaNews(boolean receiveLuppaNews) { this.receiveLuppaNews = receiveLuppaNews; }
    public boolean isReceiveEventRecommendations() { return receiveEventRecommendations; }
    public void setReceiveEventRecommendations(boolean receiveEventRecommendations) { this.receiveEventRecommendations = receiveEventRecommendations; }
    public boolean isReceiveSavedEventReminders() { return receiveSavedEventReminders; }
    public void setReceiveSavedEventReminders(boolean receiveSavedEventReminders) { this.receiveSavedEventReminders = receiveSavedEventReminders; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
