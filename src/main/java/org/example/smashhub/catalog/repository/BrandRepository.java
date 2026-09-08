package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    boolean existsBySlug(String slug);
    Optional<Brand> findBySlug(String slug);
}
