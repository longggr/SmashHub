package org.example.smashhub.entity.catalog;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.shared.enums.ProductStatus;
import org.example.smashhub.shared.enums.ProductType;
import org.example.smashhub.shared.persistence.BaseEntity;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "product")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"category", "brand"})
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseEntity {

    @Column(nullable = false)
    String name;

    @Column(nullable = false, unique = true)
    String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    ProductType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    Brand brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_status", nullable = false)
    ProductStatus productStatus;

    @Column(nullable = false,precision = 10, scale = 2)
    BigDecimal price;

    @Column(name = "sale_price", precision = 19, scale = 2)
    private BigDecimal salePrice;


}
