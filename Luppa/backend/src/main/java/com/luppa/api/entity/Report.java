package com.luppa.api.entity;

import jakarta.persistence.Column;
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
@Table(name = "denuncias")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDenuncia")
    private Integer id;

    @Column(name = "descricaoDenuncia", nullable = false, length = 350)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idEvento", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idAvaliacao")
    private Rating rating;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "idMotivo", nullable = false)
    private ReportCategory category;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "idStatusDenuncia", nullable = false)
    private StatusReport status;

    @Column(name = "dataDenuncia", nullable = false)
    private LocalDateTime reportedAt;

    @Column(name = "respostaModeracao", length = 1000)
    private String moderationResponse;

    @Column(name = "dataRespostaModeracao")
    private LocalDateTime moderationRespondedAt;

    @PrePersist
    void onCreate() {
        if (reportedAt == null) {
            reportedAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    public ReportCategory getCategory() {
        return category;
    }

    public void setCategory(ReportCategory category) {
        this.category = category;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public StatusReport getStatus() {
        return status;
    }

    public void setStatus(StatusReport status) {
        this.status = status;
    }

    public String getModerationResponse() {
        return moderationResponse;
    }

    public void setModerationResponse(String moderationResponse) {
        this.moderationResponse = moderationResponse;
    }

    public LocalDateTime getModerationRespondedAt() {
        return moderationRespondedAt;
    }

    public void setModerationRespondedAt(LocalDateTime moderationRespondedAt) {
        this.moderationRespondedAt = moderationRespondedAt;
    }
}
