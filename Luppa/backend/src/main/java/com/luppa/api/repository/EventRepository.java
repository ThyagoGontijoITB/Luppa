package com.luppa.api.repository;

import com.luppa.api.entity.Event;
import com.luppa.api.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Integer> {
    Page<Event> findByCreatorId(Integer creatorId, Pageable pageable);

    @Query("""
            select count(e.id) as totalEvents,
                   sum(case when e.status.id = 0 then 1 else 0 end) as publishedEvents,
                   sum(case when e.status.id = 1 then 1 else 0 end) as draftEvents,
                   sum(case when e.status.id = 3 then 1 else 0 end) as reviewEvents,
                   sum(case when e.status.id = 2 then 1 else 0 end) as cancelledEvents
            from Event e
            where e.creator.id = :creatorId
            """)
    EventInsightsCountView countInsightsByCreator(@Param("creatorId") Integer creatorId);

    @Query("""
            select count(e.id) as totalEvents,
                   sum(case when e.status.id = 0 then 1 else 0 end) as publishedEvents,
                   sum(case when e.status.id = 1 then 1 else 0 end) as draftEvents,
                   sum(case when e.status.id = 3 then 1 else 0 end) as reviewEvents,
                   sum(case when e.status.id = 2 then 1 else 0 end) as cancelledEvents
            from Event e
            """)
    EventInsightsCountView countPlatformInsights();

    @Query("""
            select c.description as key,
                   c.description as label,
                   count(e.id) as count
            from Event e
            join e.category c
            where e.creator.id = :creatorId
            group by c.description
            order by count(e.id) desc, c.description asc
            """)
    java.util.List<EventInsightsBreakdownView> countByCategoryForCreator(@Param("creatorId") Integer creatorId);

    @Query("""
            select c.description as key,
                   c.description as label,
                   count(e.id) as count
            from Event e
            join e.category c
            group by c.description
            order by count(e.id) desc, c.description asc
            """)
    java.util.List<EventInsightsBreakdownView> countByCategoryForPlatform();

    @Query("""
            select cast(e.status.id as string) as key,
                   e.status.description as label,
                   count(e.id) as count
            from Event e
            where e.creator.id = :creatorId
            group by e.status.id, e.status.description
            order by e.status.id asc
            """)
    java.util.List<EventInsightsBreakdownView> countByStatusForCreator(@Param("creatorId") Integer creatorId);

    @Query("""
            select cast(e.status.id as string) as key,
                   e.status.description as label,
                   count(e.id) as count
            from Event e
            group by e.status.id, e.status.description
            order by e.status.id asc
            """)
    java.util.List<EventInsightsBreakdownView> countByStatusForPlatform();

    @Query("""
            select e from Event e
            join fetch e.category
            join fetch e.address
            join fetch e.creator
            where e.destaquePrincipal = true
              and e.status.id = :publishedStatusId
            order by e.startDateTime asc
            """)
    Optional<Event> findFirstPublishedManualFeatured(@Param("publishedStatusId") Integer publishedStatusId);

    @Query("""
            select e from Event e
            join fetch e.category
            join fetch e.address
            join fetch e.creator
            where e.status.id = :publishedStatusId
              and e.startDateTime >= :now
            order by e.startDateTime asc
            """)
    java.util.List<Event> findNearestPublishedUpcoming(
            @Param("publishedStatusId") Integer publishedStatusId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            select e from Event e
            join fetch e.category
            join fetch e.address
            join fetch e.creator
            where e.id = :id
            """)
    Optional<Event> findByIdForResponse(@Param("id") Integer id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Event e set e.destaquePrincipal = false where e.destaquePrincipal = true and e.id <> :eventId")
    int clearManualFeaturedExcept(@Param("eventId") Integer eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Event e set e.destaquePrincipal = false where e.destaquePrincipal = true")
    int clearManualFeatured();

    @Query("""
            select e from Event e
            join e.category c
            join e.address a
            where (:search is null or lower(e.title) like lower(concat('%', :search, '%'))
                   or lower(a.street) like lower(concat('%', :search, '%'))
                   or lower(a.city) like lower(concat('%', :search, '%'))
                   or lower(c.description) like lower(concat('%', :search, '%')))
              and (:categoryId is null or c.id = :categoryId)
              and (:statusId is null or e.status.id = :statusId)
            """)
    Page<Event> search(
            @Param("search") String search,
            @Param("categoryId") Integer categoryId,
            @Param("statusId") Integer statusId,
            Pageable pageable
    );
}
