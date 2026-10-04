package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByColorIdOrderByOrderIndexAsc(Long colorId);
    List<ProductImage> findByColorProductIdOrderByOrderIndexAsc(Long productId);
}
