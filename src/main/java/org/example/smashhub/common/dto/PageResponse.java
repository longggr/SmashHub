package org.example.smashhub.common.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    int pageNo;
    int pageSize;
    long totalElements;
    int totalPages;
    boolean isLast;
    boolean isFirst;

    @Builder.Default
    List<T> content = Collections.emptyList();
}