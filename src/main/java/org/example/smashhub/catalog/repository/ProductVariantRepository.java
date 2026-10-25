package org.example.smashhub.catalog.repository;

import jakarta.persistence.LockModeType;
import org.example.smashhub.catalog.entity.ProductVariant;
import org.example.smashhub.common.enums.InventoryStatus;
import org.example.smashhub.common.enums.VariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    boolean existsBySku(String sku);
    boolean existsByColorId(Long colorId);
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByColorId(Long colorId);
    List<ProductVariant> findByColorProductId(Long productId);

    Optional<ProductVariant> findByIdAndColorProductId(Long id, Long productId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.stock = v.stock - :qty " +
            "WHERE v.id = :id AND v.stock >= :qty AND v.active = :active")
    int deductStockAtomic(@Param("id") Long id,
                          @Param("qty") int qty,
                          @Param("active") VariantStatus active);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.stock = v.stock + :qty WHERE v.id = :id")
    int restoreStockAtomic(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.inventoryStatus = :status WHERE v.id = :id AND v.stock = 0")
    int markOutOfStockIfEmpty(@Param("id") Long id, @Param("status") InventoryStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.inventoryStatus = :status " +
            "WHERE v.id = :id AND v.stock > 0 AND (v.inventoryStatus IS NULL OR v.inventoryStatus <> :status)")
    int markInStockIfAvailable(@Param("id") Long id, @Param("status") InventoryStatus status);
}