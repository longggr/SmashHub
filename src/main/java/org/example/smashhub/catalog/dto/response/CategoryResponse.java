package org.example.smashhub.catalog.dto.response;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

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
public class CategoryResponse implements Serializable {
    Long id;
    String name;
    String slug;
    Long parentId;
    String parentName;
    Integer displayOrder;
    @Builder.Default
    List<CategoryResponse> children = Collections.emptyList();
}
