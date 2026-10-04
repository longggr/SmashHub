package org.example.smashhub.catalog.dto.response;

import java.io.Serializable;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.enums.InventoryStatus;
import org.example.smashhub.common.enums.VariantStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantResponse implements Serializable {
    Long id;
    String sku;
    String size;
    Integer stock;
    VariantStatus active;
    InventoryStatus inventoryStatus;
}
