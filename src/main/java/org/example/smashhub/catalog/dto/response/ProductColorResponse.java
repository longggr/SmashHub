package org.example.smashhub.catalog.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductColorResponse implements Serializable {
    Long id;
    String colorName;
    String hexCode;
    Integer displayOrder;
    @Builder.Default
    List<ProductImageResponse> images = Collections.emptyList();
    @Builder.Default
    List<ProductVariantResponse> variants = Collections.emptyList();
}
