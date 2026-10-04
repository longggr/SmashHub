package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.ProductColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductColorRepository extends JpaRepository<ProductColor, Long> {
    List<ProductColor> findByProductIdOrderByDisplayOrderAsc(Long productId);
}
