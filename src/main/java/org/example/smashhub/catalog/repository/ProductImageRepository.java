package org.example.smashhub.catalog.repository;

import org.example.smashhub.catalog.entity.ProductImage;
import org.example.smashhub.common.enums.ImagePriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByColorIdOrderByOrderIndexAsc(Long colorId);
    List<ProductImage> findByColorProductIdOrderByOrderIndexAsc(Long productId);

    Optional<ProductImage> findByIdAndColorProductId(Long id, Long productId);
    @Query("SELECT COUNT(i) > 0 FROM ProductImage i WHERE i.color.id = :colorId AND i.isMain = :priority")
    boolean hasImageWithPriority(@Param("colorId") Long colorId, @Param("priority") ImagePriority priority);


    @Query("SELECT c.product.id, i.url, i.isMain FROM ProductImage i JOIN i.color c " +
            "WHERE c.product.id IN :productIds ORDER BY c.displayOrder ASC, i.orderIndex ASC")
    List<Object[]> findThumbnailCandidates(@Param("productIds") Collection<Long> productIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductImage i SET i.isMain = :secondary WHERE i.color.id = :colorId AND i.id <> :keepId")
    int demoteOthers(@Param("colorId") Long colorId,
                     @Param("keepId") Long keepId,
                     @Param("secondary") ImagePriority secondary);
}