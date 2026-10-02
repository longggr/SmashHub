package org.example.smashhub.catalog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.enums.ProductStatus;
import org.example.smashhub.common.enums.ProductType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductRequest {
    @NotBlank(message = "PRODUCT_NAME_INVALID")
    String name;

    @NotBlank(message = "PRODUCT_SLUG_INVALID")
    String slug;

    @NotBlank(message = "PRODUCT_DESCRIPTION_INVALID")
    String description;

    @NotNull(message = "PRODUCT_TYPE_INVALID")
    ProductType type;

    @NotNull(message = "PRODUCT_CATEGORY_INVALID")
    Long categoryId;

    @NotNull(message = "PRODUCT_BRAND_INVALID")
    Long brandId;

    @NotNull(message = "PRODUCT_STATUS_INVALID")
    ProductStatus productStatus;

    @NotNull(message = "PRODUCT_PRICE_INVALID")
    @DecimalMin(value = "0.0", inclusive = false, message = "PRODUCT_PRICE_INVALID")
    BigDecimal price;

    BigDecimal salePrice;
}
