package com.luppa.api.repository;

import com.luppa.api.entity.SavedEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedEventRepository extends JpaRepository<SavedEvent, Integer> {
    boolean existsByEventId(Integer eventId);

    interface InterestCount {
        Integer getEventId();
        long getTotal();
    }

    @Query("select s.event.id as eventId, count(distinct s.user.id) as total from SavedEvent s where s.event.id in :ids group by s.event.id")
    List<InterestCount> countInterests(@Param("ids") java.util.Collection<Integer> ids);

    @Query("select count(distinct s.user.id) from SavedEvent s where s.event.id = :id")
    long countInterest(@Param("id") Integer id);

    boolean existsByEventIdAndUserId(Integer eventId, Integer userId);
    Optional<SavedEvent> findByEventIdAndUserId(Integer eventId, Integer userId);
    List<SavedEvent> findByUserIdOrderBySavedAtDesc(Integer userId);
    void deleteByEventIdAndUserId(Integer eventId, Integer userId);

    @Query("""
            select count(s.id)
            from SavedEvent s
            where s.event.creator.id = :creatorId
            """)
    long countByEventCreatorId(@Param("creatorId") Integer creatorId);

    @Query("select count(s.id) from SavedEvent s")
    long countAllSavedEvents();

    @Query("""
            select s.event.id as eventId,
                   s.event.title as title,
                   s.event.category.description as categoryName,
                   s.event.status.id as statusId,
                   count(s.id) as saveCount,
                   0L as reviewCount,
                   null as averageRating
            from SavedEvent s
            where s.event.creator.id = :creatorId
            group by s.event.id, s.event.title, s.event.category.description, s.event.status.id
            order by count(s.id) desc, s.event.title asc
            """)
    List<EventInsightsRankedEventView> findMostSavedByCreator(@Param("creatorId") Integer creatorId, Pageable pageable);

    @Query("""
            select s.event.id as eventId,
                   s.event.title as title,
                   s.event.category.description as categoryName,
                   s.event.status.id as statusId,
                   count(s.id) as saveCount,
                   0L as reviewCount,
                   null as averageRating
            from SavedEvent s
            group by s.event.id, s.event.title, s.event.category.description, s.event.status.id
            order by count(s.id) desc, s.event.title asc
            """)
    List<EventInsightsRankedEventView> findMostSavedForPlatform(Pageable pageable);
}
