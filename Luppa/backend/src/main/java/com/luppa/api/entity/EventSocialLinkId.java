package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EventSocialLinkId implements Serializable {
    @Column(name = "idEvento")
    private Integer eventId;

    @Column(name = "idRede")
    private Integer socialProfileId;

    protected EventSocialLinkId() {
    }

    public EventSocialLinkId(Integer eventId, Integer socialProfileId) {
        this.eventId = eventId;
        this.socialProfileId = socialProfileId;
    }

    public Integer getEventId() {
        return eventId;
    }

    public Integer getSocialProfileId() {
        return socialProfileId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EventSocialLinkId that)) {
            return false;
        }
        return Objects.equals(eventId, that.eventId)
                && Objects.equals(socialProfileId, that.socialProfileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, socialProfileId);
    }
}
