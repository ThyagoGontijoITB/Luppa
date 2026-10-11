package com.luppa.api.repository;

public interface EventInsightsRankedEventView {
    Integer getEventId();
    String getTitle();
    String getCategoryName();
    Integer getStatusId();
    Long getSaveCount();
    Long getReviewCount();
    Double getAverageRating();
}
