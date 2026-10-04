package org.example.smashhub.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.enums.InventoryStatus;
import org.example.smashhub.common.enums.VariantStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantRequest {
    @NotBlank(message = "PRODUCT_VARIANT_SKU_INVALID")
    String sku;
    String size;

    @NotNull(message = "PRODUCT_VARIANT_STOCK_INVALID")
    @PositiveOrZero(message = "PRODUCT_VARIANT_STOCK_INVALID")
    Integer stock;

    VariantStatus active;
    InventoryStatus inventoryStatus;

    @NotNull(message = "PRODUCT_COLOR_NOT_FOUND")
    Long colorId;
}