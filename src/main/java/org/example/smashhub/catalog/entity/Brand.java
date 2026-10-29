package org.example.smashhub.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.persistence.BaseEntity;

@Data
@Entity
@Table(name = "brand")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Brand extends BaseEntity {

    @Column(nullable = false)
    String name;

    @Column(nullable = false, unique = true)
    String slug;

    @Column(name = "logo_url", length = 1024)
    String logoUrl;

    /**
     * Public ID của ảnh logo trên Cloudinary.
     * Dùng để xóa ảnh cũ khi logo được thay thế hoặc brand bị xóa.
     * Ưu tiên trường này thay vì phân tích URL (URL có thể bị transform/biến đổi).
     */
    @Column(name = "logo_public_id")
    String logoPublicId;
}
