package org.example.smashhub.catalog.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.enums.ProductStatus;
import org.example.smashhub.common.enums.ProductType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse implements Serializable {
    Long id;
    String name;
    String slug;
    String description;
    ProductType type;
    ProductStatus productStatus;
    BigDecimal price;
    BigDecimal salePrice;
    BrandResponse brand;
    CategoryResponse category;
    @Builder.Default
    List<ProductAttributeResponse> attributes = Collections.emptyList();
    @Builder.Default
    List<ProductColorResponse> colors = Collections.emptyList();
    LocalDateTime createDate;
    LocalDateTime updateDate;

}
