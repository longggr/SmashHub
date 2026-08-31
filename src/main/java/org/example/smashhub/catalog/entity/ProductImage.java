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
import org.example.smashhub.common.enums.ImagePriority;
import org.example.smashhub.common.persistence.BaseEntity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"color"})
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_images")
public class ProductImage extends BaseEntity {

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "provider_public_id", nullable = false)
    private String providerPublicId;

    @Column(name = "title")
    private String title;

    @Column(name = "alt_text", length = 512)
    private String altText;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_main")
    private ImagePriority isMain;

    @Column(name = "order_index")
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id", nullable = false)
    private ProductColor color;
}
