package org.example.smashhub.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
    boolean isFirst;
    boolean isLast;
    List<T> content;
}