package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    boolean existsBySku(String sku);
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByColorId(Long colorId);
    List<ProductVariant> findByColorProductId(Long productId);
}
