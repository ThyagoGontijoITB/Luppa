package com.luppa.api.repository;

public interface EventInsightsCountView {
    Long getTotalEvents();
    Long getPublishedEvents();
    Long getDraftEvents();
    Long getReviewEvents();
    Long getCancelledEvents();
}
