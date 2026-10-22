package com.luppa.api.repository;

import com.luppa.api.entity.EventSocialLink;
import com.luppa.api.entity.EventSocialLinkId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventSocialLinkRepository extends JpaRepository<EventSocialLink, EventSocialLinkId> {
    boolean existsByIdEventId(Integer eventId);

    List<EventSocialLink> findByIdEventId(Integer eventId);
}
