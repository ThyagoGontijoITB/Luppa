package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "evento")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idEvento")
    private Integer id;

    @Column(name = "titulo", nullable = false, length = 40)
    private String title;

    @Column(name = "descricao", nullable = false, length = 150)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idCategoria", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "idEndereco", nullable = false)
    private Address address = Address.fromLocation(null);

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "idContato", nullable = false)
    private EventContact contact;

    @Transient
    private java.math.BigDecimal latitude;

    @Transient
    private java.math.BigDecimal longitude;

    @Column(name = "dataInicio", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "dataFinal", nullable = false)
    private LocalDateTime endDateTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "idStatus", nullable = false)
    private StatusEvent status = StatusEvent.of(EventStatus.DRAFT);

    @Column(name = "pathImagemCapa", nullable = false, length = 200)
    private String coverImagePath = "";

    @Column(name = "destaquePrincipal", nullable = false)
    private boolean destaquePrincipal;

    @Transient
    private EventImage coverImage;

    @Column(name = "dataPostagem", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = truncate(title, 40); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = truncate(description, 150); }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getLocation() { return address == null ? "" : address.display(); }
    public void setLocation(String location) { this.address = Address.fromLocation(location); }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address == null ? Address.fromLocation(null) : address; }
    public java.math.BigDecimal getLatitude() { return latitude; }
    public void setLatitude(java.math.BigDecimal latitude) { this.latitude = latitude; }
    public java.math.BigDecimal getLongitude() { return longitude; }
    public void setLongitude(java.math.BigDecimal longitude) { this.longitude = longitude; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    public EventStatus getStatus() { return status.getStatus(); }
    public void setStatus(EventStatus status) { this.status = StatusEvent.of(status); }
    public EventImage getCoverImage() {
        return coverImagePath == null || coverImagePath.isBlank() ? null : EventImage.fromPath(id, coverImagePath);
    }
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath == null ? "" : truncate(coverImagePath, 200); }
    public String getCoverImagePath() { return coverImagePath; }
    public boolean isDestaquePrincipal() { return destaquePrincipal; }
    public void setDestaquePrincipal(boolean destaquePrincipal) { this.destaquePrincipal = destaquePrincipal; }
    public void setContact(EventContact contact) { this.contact = contact; }
    public Instant getCreatedAt() { return createdAt == null ? null : createdAt.toInstant(ZoneOffset.UTC); }
    public Instant getUpdatedAt() { return null; }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
