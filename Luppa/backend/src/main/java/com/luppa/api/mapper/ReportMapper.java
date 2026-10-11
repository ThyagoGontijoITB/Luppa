package com.luppa.api.mapper;

import com.luppa.api.dto.ReportCategoryResponse;
import com.luppa.api.dto.PublicUserResponse;
import com.luppa.api.dto.ReportRatingTargetResponse;
import com.luppa.api.dto.ReportResponse;
import com.luppa.api.entity.Rating;
import com.luppa.api.entity.Report;
import com.luppa.api.entity.ReportCategory;

public final class ReportMapper {
    private ReportMapper() {
    }

    public static ReportCategoryResponse toCategoryResponse(ReportCategory category) {
        return new ReportCategoryResponse(
                category.getId().longValue(),
                category.getCategory()
        );
    }

    public static ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId().longValue(),
                report.getEvent().getId().longValue(),
                report.getEvent().getTitle(),
                report.getRating() == null ? null : report.getRating().getId().longValue(),
                toRatingTargetResponse(report.getRating()),
                UserMapper.toResponse(report.getReporter()),
                toCategoryResponse(report.getCategory()),
                report.getDescription(),
                report.getReportedAt(),
                report.getStatus().getStatus(),
                report.getModerationResponse(),
                report.getModerationRespondedAt()
        );
    }

    private static ReportRatingTargetResponse toRatingTargetResponse(Rating rating) {
        if (rating == null) {
            return null;
        }
        return new ReportRatingTargetResponse(
                rating.getId().longValue(),
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
}
