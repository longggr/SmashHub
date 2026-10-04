package org.example.smashhub.catalog.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.enums.ImagePriority;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImageResponse implements Serializable {
    Long id;
    String url;
    String providerPublicId;
    String title;
    String altText;
    ImagePriority isMain;
    Integer orderIndex;
}
