package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {
    List<ProductAttribute> findByProductId(Long productId);

    /** Tìm attribute NHƯNG phải thuộc đúng product -> chặn sửa/xóa nhầm attribute của sản phẩm khác (IDOR). */
    Optional<ProductAttribute> findByIdAndProductId(Long id, Long productId);
}