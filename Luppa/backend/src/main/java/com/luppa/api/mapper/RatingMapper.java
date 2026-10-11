package com.luppa.api.mapper;

import com.luppa.api.dto.RatingResponse;
import com.luppa.api.dto.PublicUserResponse;
import com.luppa.api.entity.Rating;
import com.luppa.api.repository.EventRatingView;

public final class RatingMapper {
    private RatingMapper() {
    }

    public static RatingResponse toResponse(Rating rating) {
        return new RatingResponse(
                rating.getId().longValue(),
                rating.getEvent().getId().longValue(),
                new PublicUserResponse(
                        rating.getUser().getId().longValue(),
                        rating.getUser().getName(),
                        UserMapper.toProfileImageUrl(rating.getUser().getProfileImagePath())
                ),
                Byte.toUnsignedInt(rating.getScore()),
                rating.getDescription(),
                rating.getRatedAt()
        );
    }

    public static RatingResponse toResponse(EventRatingView rating) {
        String name = ((rating.getFirstName() == null ? "" : rating.getFirstName())
                + " "
                + (rating.getLastName() == null ? "" : rating.getLastName())).trim();
        return new RatingResponse(
                rating.getId().longValue(),
                rating.getEventId().longValue(),
                new PublicUserResponse(
                        rating.getUserId().longValue(),
                        name,
                        UserMapper.toProfileImageUrl(rating.getProfileImagePath())
                ),
                Byte.toUnsignedInt(rating.getScore()),
                rating.getDescription(),
                rating.getRatedAt()
        );
    }
}
