package org.example.smashhub.catalog.dto.request;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.enums.ProductStatus;
import org.example.smashhub.common.enums.ProductType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSearchRequest {
    String keyword;
    Long categoryId;
    Long BrandId;
    ProductType type;
    ProductStatus status;
    BigDecimal minPrice;
    BigDecimal maxPrice;
    Boolean onSale;
    @Builder.Default
    List<ProductAttributeFilter> attributes = Collections.emptyList();
}
