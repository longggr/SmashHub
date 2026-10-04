package org.example.smashhub.catalog.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.enums.ProductStatus;
import org.example.smashhub.common.enums.ProductType;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSummaryResponse implements Serializable {
    Long id;
    String slug;
    ProductType type;
    String categoryName;
    String brandName;
    ProductStatus productStatus;
    BigDecimal price;
    BigDecimal salePrice;
}
