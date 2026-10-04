package org.example.smashhub.catalog.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.enums.ImagePriority;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImageRequest {
    @NotBlank(message = "PRODUCT_IMAGE_URL_INVALID")
    String url;
    String providerPublicId;
    String title;
    String altText;
    ImagePriority isMain;
    Integer orderIndex;

    @NotNull(message = "PRODUCT_COLOR_NOT_FOUND")
    Long colorId;
}
