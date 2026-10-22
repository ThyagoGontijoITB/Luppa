package com.luppa.api.repository;

import com.luppa.api.entity.Rating;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Integer> {
    boolean existsByEventId(Integer eventId);

    Optional<Rating> findByEventIdAndUserId(Integer eventId, Integer userId);
    List<Rating> findByEventIdOrderByRatedAtDesc(Integer eventId);
    @Query("""
            select r.id as id,
                   r.event.id as eventId,
                   r.user.id as userId,
                   r.user.firstName as firstName,
                   r.user.lastName as lastName,
                   r.user.profileImagePath as profileImagePath,
                   r.score as score,
                   r.description as description,
                   r.ratedAt as ratedAt
            from Rating r
            where r.event.id = :eventId
            order by r.ratedAt desc
            """)
    List<EventRatingView> findPublicByEventIdOrderByRatedAtDesc(@Param("eventId") Integer eventId);
    List<Rating> findByUserIdOrderByRatedAtDesc(Integer userId);
    void deleteByEventIdAndUserId(Integer eventId, Integer userId);

    @Query("""
            select count(r.id)
            from Rating r
            where r.event.creator.id = :creatorId
            """)
    long countByEventCreatorId(@Param("creatorId") Integer creatorId);

    @Query("""
            select avg(r.score)
            from Rating r
            where r.event.creator.id = :creatorId
            """)
    Double averageScoreByEventCreatorId(@Param("creatorId") Integer creatorId);

    @Query("select avg(r.score) from Rating r")
    Double averageScoreForPlatform();

    @Query("""
            select r.event.id as eventId,
                   r.event.title as title,
                   r.event.category.description as categoryName,
                   r.event.status.id as statusId,
                   0L as saveCount,
                   count(r.id) as reviewCount,
                   avg(r.score) as averageRating
            from Rating r
            where r.event.creator.id = :creatorId
            group by r.event.id, r.event.title, r.event.category.description, r.event.status.id
            order by avg(r.score) desc, count(r.id) desc, r.event.title asc
            """)
    List<EventInsightsRankedEventView> findBestRatedByCreator(@Param("creatorId") Integer creatorId, Pageable pageable);

    @Query("""
            select r.event.id as eventId,
                   r.event.title as title,
                   r.event.category.description as categoryName,
                   r.event.status.id as statusId,
                   0L as saveCount,
                   count(r.id) as reviewCount,
                   avg(r.score) as averageRating
            from Rating r
            group by r.event.id, r.event.title, r.event.category.description, r.event.status.id
            order by avg(r.score) desc, count(r.id) desc, r.event.title asc
            """)
    List<EventInsightsRankedEventView> findBestRatedForPlatform(Pageable pageable);
}
