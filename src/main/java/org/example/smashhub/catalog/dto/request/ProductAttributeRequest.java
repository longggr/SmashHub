package org.example.smashhub.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductAttributeRequest {
    @NotBlank(message = "PRODUCT_ATTR_NAME_INVALID")
    String attrName;

    @NotBlank(message = "PRODUCT_ATTR_VALUE_INVALID")
    String attrValue;
}
