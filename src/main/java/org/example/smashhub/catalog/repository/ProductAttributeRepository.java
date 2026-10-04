package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute,Long> {
    List<ProductAttribute> findByProductId(Long productId);
}
