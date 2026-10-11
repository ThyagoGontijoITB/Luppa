package com.luppa.api.repository;

import java.time.LocalDateTime;

public interface EventRatingView {
    Integer getId();
    Integer getEventId();
    Integer getUserId();
    String getFirstName();
    String getLastName();
    String getProfileImagePath();
    Byte getScore();
    String getDescription();
    LocalDateTime getRatedAt();
}
