package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.ProductColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductColorRepository extends JpaRepository<ProductColor, Long> {
    List<ProductColor> findByProductIdOrderByDisplayOrderAsc(Long productId);

    /** Màu phải thuộc đúng product (chặn thao tác chéo sản phẩm). */
    Optional<ProductColor> findByIdAndProductId(Long id, Long productId);

    boolean existsByProductIdAndColorNameIgnoreCase(Long productId, String colorName);

    boolean existsByProductIdAndColorNameIgnoreCaseAndIdNot(Long productId, String colorName, Long id);
}