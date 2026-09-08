package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.Product;
import org.example.smashhub.common.enums.ProductStatus;
import org.example.smashhub.common.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySlug(String slug);
    Optional<Product> findBySlug(String slug);
    @Query("SELECT p FROM Product p WHERE " +
            "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:brandId IS NULL OR p.brand.id = :brandId) AND " +
            "(:type IS NULL OR p.type = :type) AND " +
            "(:status IS NULL OR p.productStatus = :status)")
    Page<Product> search(@Param("keyword") String keyword,
                         @Param("categoryId") Long categoryId,
                         @Param("brandId") Long brandId,
                         @Param("type") ProductType type,
                         @Param("status") ProductStatus status,
                         Pageable pageable);
}
