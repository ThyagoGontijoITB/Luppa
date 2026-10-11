package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "statusEvento")
public class StatusEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idStatus")
    private Integer id;

    @Column(name = "descricao", nullable = false, length = 200)
    private String description;

    public Integer getId() {
        return id;
    }

    public EventStatus getStatus() {
        if (id == 0) return EventStatus.PUBLISHED;
        if (id == 1) return EventStatus.DRAFT;
        if (id == 2) return EventStatus.CANCELLED;
        if (id == 3) return EventStatus.REVIEW;
        throw new IllegalStateException("Unknown statusEvento idStatus: " + id);
    }

    public static StatusEvent of(EventStatus status) {
        StatusEvent entity = new StatusEvent();
        entity.id = idFor(status);
        entity.description = descriptionFor(status);
        return entity;
    }

    public static int idFor(EventStatus status) {
        if (status == EventStatus.PUBLISHED) return 0;
        if (status == EventStatus.DRAFT) return 1;
        if (status == EventStatus.CANCELLED) return 2;
        if (status == EventStatus.REVIEW) return 3;
        throw new IllegalStateException("Unknown EventStatus: " + status);
    }

    private static String descriptionFor(EventStatus status) {
        if (status == EventStatus.PUBLISHED) return "Evento publicado.";
        if (status == EventStatus.DRAFT) return "Evento em rascunho.";
        if (status == EventStatus.CANCELLED) return "Evento cancelado.";
        if (status == EventStatus.REVIEW) return "Evento em revisão.";
        throw new IllegalStateException("Unknown EventStatus: " + status);
    }
}
