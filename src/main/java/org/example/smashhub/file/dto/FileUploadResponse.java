package org.example.smashhub.file.dto;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileUploadResponse {
    String url;
    String publicId;
    String format;
    Long bytes;
    Integer width;
    Integer height;
}
