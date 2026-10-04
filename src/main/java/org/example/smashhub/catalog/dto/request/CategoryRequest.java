package org.example.smashhub.catalog.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryRequest {
    @NotBlank(message = "CATEGORY_NAME_INVALID")
    String name;

    @NotBlank(message = "CATEGORY_SLUG_INVALID")
    String slug;

    Long parentId;

    @PositiveOrZero(message = "CATEGORY_DISPLAY_ORDER_INVALID")
    Integer displayOrder;
}
