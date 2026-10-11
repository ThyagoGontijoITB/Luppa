package com.luppa.api.repository;

import com.luppa.api.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query(
            value = """
                    select l from AuditLog l
                    left join fetch l.user u
                    where (:search is null
                           or lower(l.tableName) like lower(concat('%', :search, '%'))
                           or lower(l.operation) like lower(concat('%', :search, '%'))
                           or (u is not null and lower(u.firstName) like lower(concat('%', :search, '%')))
                           or (u is not null and lower(u.lastName) like lower(concat('%', :search, '%')))
                           or (u is not null and lower(u.email) like lower(concat('%', :search, '%')))
                           or (:searchInteger is not null and l.recordId = :searchInteger)
                           or (:searchInteger is not null and coalesce(l.historicalUserId, l.userId) = :searchInteger)
                           or (:searchLong is not null and l.id = :searchLong))
                      and (:operation is null or l.operation = :operation)
                      and (:tableName is null or l.tableName = :tableName)
                      and (:user is null
                           or (:userInteger is not null and coalesce(l.historicalUserId, l.userId) = :userInteger)
                           or (u is not null and lower(u.firstName) like lower(concat('%', :user, '%')))
                           or (u is not null and lower(u.lastName) like lower(concat('%', :user, '%')))
                           or (u is not null and lower(u.email) like lower(concat('%', :user, '%'))))
                      and (:startAt is null or l.operatedAt >= :startAt)
                      and (:endAt is null or l.operatedAt < :endAt)
                    """,
            countQuery = """
                    select count(l) from AuditLog l
                    left join l.user u
                    where (:search is null
                           or lower(l.tableName) like lower(concat('%', :search, '%'))
                           or lower(l.operation) like lower(concat('%', :search, '%'))
                           or (u is not null and lower(u.firstName) like lower(concat('%', :search, '%')))
                           or (u is not null and lower(u.lastName) like lower(concat('%', :search, '%')))
                           or (u is not null and lower(u.email) like lower(concat('%', :search, '%')))
                           or (:searchInteger is not null and l.recordId = :searchInteger)
                           or (:searchInteger is not null and coalesce(l.historicalUserId, l.userId) = :searchInteger)
                           or (:searchLong is not null and l.id = :searchLong))
                      and (:operation is null or l.operation = :operation)
                      and (:tableName is null or l.tableName = :tableName)
                      and (:user is null
                           or (:userInteger is not null and coalesce(l.historicalUserId, l.userId) = :userInteger)
                           or (u is not null and lower(u.firstName) like lower(concat('%', :user, '%')))
                           or (u is not null and lower(u.lastName) like lower(concat('%', :user, '%')))
                           or (u is not null and lower(u.email) like lower(concat('%', :user, '%'))))
                      and (:startAt is null or l.operatedAt >= :startAt)
                      and (:endAt is null or l.operatedAt < :endAt)
                    """
    )
    Page<AuditLog> search(
            @Param("search") String search,
            @Param("searchInteger") Integer searchInteger,
            @Param("searchLong") Long searchLong,
            @Param("operation") String operation,
            @Param("tableName") String tableName,
            @Param("user") String user,
            @Param("userInteger") Integer userInteger,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            Pageable pageable
    );

    @Query("select distinct l.tableName from AuditLog l order by l.tableName")
    List<String> findDistinctTableNames();
}
