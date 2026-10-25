package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.Product;
import org.example.smashhub.common.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsBySlug(String slug);
    Optional<Product> findBySlug(String slug);
    boolean existsByBrandId(Long brandId);
    boolean existsByCategoryId(Long categoryId);
    long countByBrandIdAndProductStatus(Long brandId, ProductStatus status);

    @Query("SELECT p.brand.id, COUNT(p.id) FROM Product p WHERE p.productStatus = :status GROUP BY p.brand.id")
    List<Object[]> countProductsGroupByBrand(@Param("status") ProductStatus status);

    /**
     * Ghi đè findAll(spec, pageable) để nạp luôn category + brand bằng JOIN FETCH.
     * Nếu không, mapper đọc product.getCategory().getName() / getBrand().getName() sẽ bắn thêm query lazy cho mỗi dòng.
     */
    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
}