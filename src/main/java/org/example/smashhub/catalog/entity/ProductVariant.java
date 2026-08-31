package org.example.smashhub.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.example.smashhub.common.enums.InventoryStatus;
import org.example.smashhub.common.enums.VariantStatus;
import org.example.smashhub.common.persistence.BaseEntity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"color"})
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "size", length = 50)
    private String size;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "active")
    private VariantStatus active;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", length = 20)
    private InventoryStatus inventoryStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id", nullable = false)
    private ProductColor color;
}
