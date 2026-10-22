package com.luppa.api.service;

import com.luppa.api.exception.ConflictException;
import com.luppa.api.repository.*;
import org.springframework.stereotype.Component;

@Component
public class EventDeletionGuard {
    private final SavedEventRepository saves;
    private final RatingRepository ratings;
    private final ReportRepository reports;
    private final EventSocialLinkRepository links;
    public EventDeletionGuard(SavedEventRepository saves, RatingRepository ratings, ReportRepository reports, EventSocialLinkRepository links) {
        this.saves = saves; this.ratings = ratings; this.reports = reports; this.links = links;
    }
    public void check(Integer eventId) {
        if (saves.existsByEventId(eventId) || ratings.existsByEventId(eventId)
                || reports.existsByEventId(eventId) || links.existsByIdEventId(eventId)) {
            throw new ConflictException("Este evento possui interessados, avaliações, denúncias ou links vinculados. Cancele o evento em Editar para preservar seu histórico.");
        }
    }
}
