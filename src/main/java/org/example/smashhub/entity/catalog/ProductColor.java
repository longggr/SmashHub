package org.example.smashhub.entity.catalog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.example.smashhub.shared.persistence.BaseEntity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"product"})
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_colors")
public class ProductColor extends BaseEntity {

    @Column(name = "color_name", nullable = false)
    private String colorName;

    @Column(name = "hex_code", length = 20)
    private String hexCode;

    @Column(name = "display_order")
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
