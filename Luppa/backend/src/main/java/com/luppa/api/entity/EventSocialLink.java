package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "redesSociaisEvento")
public class EventSocialLink {
    @EmbeddedId
    private EventSocialLinkId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("eventId")
    @JoinColumn(name = "idEvento", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @MapsId("socialProfileId")
    @JoinColumn(name = "idRede", nullable = false)
    private SocialProfile socialProfile;

    @Column(name = "urlPerfil", nullable = false, length = 250)
    private String url;

    public EventSocialLinkId getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public SocialProfile getSocialProfile() {
        return socialProfile;
    }

    public String getUrl() {
        return url;
    }
}
