package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    boolean existsBySlug(String slug);
    Optional<Category> findBySlug(String slug);
    List<Category> findByParentIsNull();
    List<Category> findByParentId(Long parentId);
}
