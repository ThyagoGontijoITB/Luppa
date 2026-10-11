package com.luppa.api.repository;

import com.luppa.api.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    default Optional<Category> findBySlug(String slug) {
        return findAll().stream()
                .filter(category -> category.getSlug().equalsIgnoreCase(slug))
                .findFirst();
    }

    boolean existsByDescriptionIgnoreCase(String description);

    default boolean existsByNameIgnoreCase(String name) {
        return existsByDescriptionIgnoreCase(name);
    }

    default boolean existsBySlug(String slug) {
        return findBySlug(slug).isPresent();
    }
}
