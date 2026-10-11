package com.luppa.api.controller;

import com.luppa.api.dto.CategoryResponse;
import com.luppa.api.dto.EventResponse;
import com.luppa.api.dto.PageResponse;
import com.luppa.api.entity.EventStatus;
import com.luppa.api.service.CategoryService;
import com.luppa.api.service.EventService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;
    private final EventService eventService;

    public CategoryController(CategoryService categoryService, EventService eventService) {
        this.categoryService = categoryService;
        this.eventService = eventService;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.findAll();
    }

    @GetMapping("/{slug}")
    public CategoryResponse get(@PathVariable String slug) {
        return categoryService.findBySlug(slug);
    }

    @GetMapping("/{slug}/events")
    public PageResponse<EventResponse> events(@PathVariable String slug, Pageable pageable) {
        categoryService.findBySlug(slug);
        return PageResponse.from(eventService.search(null, slug, EventStatus.PUBLISHED, pageable));
    }
}
