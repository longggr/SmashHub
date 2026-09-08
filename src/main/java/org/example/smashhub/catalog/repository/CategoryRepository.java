package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsBySlug(String slug);
    Optional<Category> findBySlug(String slug);
    List<Category> findByParentIsNull();
    List<Category> findByParentId(Long parentId);
}
