package com.luppa.api.controller;

import com.luppa.api.dto.ReportCategoryResponse;
import com.luppa.api.dto.ReportCreateRequest;
import com.luppa.api.dto.ReportResponse;
import com.luppa.api.dto.ReportStatusUpdateRequest;
import com.luppa.api.security.AuthUser;
import com.luppa.api.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports/categories")
    public List<ReportCategoryResponse> categories() {
        return reportService.categories();
    }

    @PostMapping("/events/{eventId}/reports")
    public ReportResponse create(@PathVariable Integer eventId, @Valid @RequestBody ReportCreateRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return reportService.create(eventId, request, authUser);
    }

    @GetMapping("/reports")
    public List<ReportResponse> all() {
        return reportService.findAll();
    }

    @GetMapping("/events/{eventId}/reports")
    public List<ReportResponse> byEvent(@PathVariable Integer eventId) {
        return reportService.findByEvent(eventId);
    }

    @GetMapping("/me/reports")
    public List<ReportResponse> mine(@AuthenticationPrincipal AuthUser authUser) {
        return reportService.findMine(authUser);
    }

    @PutMapping("/reports/{reportId}/status")
    public ReportResponse updateStatus(@PathVariable Integer reportId, @Valid @RequestBody ReportStatusUpdateRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return reportService.updateStatus(reportId, request, authUser);
    }
}
