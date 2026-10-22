package com.luppa.api.repository;

import com.luppa.api.entity.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Integer> {
    boolean existsByEventId(Integer eventId);

    @Override
    @EntityGraph(attributePaths = {"event", "reporter", "reporter.role", "category", "status", "rating", "rating.user"})
    List<Report> findAll();

    @EntityGraph(attributePaths = {"event", "reporter", "reporter.role", "category", "status", "rating", "rating.user"})
    List<Report> findByEventIdOrderByReportedAtDesc(Integer eventId);

    @EntityGraph(attributePaths = {"event", "reporter", "reporter.role", "category", "status", "rating", "rating.user"})
    List<Report> findByReporterIdOrderByReportedAtDesc(Integer reporterId);
}
