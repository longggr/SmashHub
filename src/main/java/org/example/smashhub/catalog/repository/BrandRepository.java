package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.Brand;
import org.example.smashhub.common.enums.ProductStatus;
import org.example.smashhub.common.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    boolean existsBySlug(String slug);
    Optional<Brand> findBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
    @Query("SELECT b FROM Brand b WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.slug) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Brand> searchBrands(@Param("keyword") String keyword, Pageable pageable);
    @Query("SELECT DISTINCT b FROM Brand b JOIN Product p ON p.brand.id = b.id " +
            "WHERE p.category.id IN :categoryIds AND p.productStatus = :status " +
            "ORDER BY b.name ASC")
    List<Brand> findActiveBrandsByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds,
                                              @Param("status") ProductStatus status);
    @Query("SELECT DISTINCT b FROM Brand b JOIN Product p ON p.brand.id = b.id " +
            "WHERE p.type = :type AND p.productStatus = :status " +
            "ORDER BY b.name ASC")
    List<Brand> findActiveBrandsByProductType(@Param("type") ProductType type,
                                              @Param("status") ProductStatus status);
}
