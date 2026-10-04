package org.example.smashhub.catalog.dto.response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrandResponse implements Serializable {
    Long id;
    String name;
    String slug;
    String logoUrl;
    LocalDateTime createDate;
    LocalDateTime updateDate;

}
