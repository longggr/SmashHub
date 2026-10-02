package org.example.smashhub.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.enums.InventoryStatus;
import org.example.smashhub.common.enums.VariantStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantRequest {
    @NotBlank(message = "PRODUCT_VARIANT_SKU_INVALID")
    String sku;
    String size;

    // -------------------------------------------------------------------------
    // VALIDATION CHO TỒN KHO (STOCK)
    // -------------------------------------------------------------------------
    // Tại sao phải dùng cả 2 annotation?
    // 1. @NotNull: Ngăn chặn việc Frontend gửi lên thiếu trường "stock" hoặc cố tình truyền null.
    // 2. @PositiveOrZero: Ngăn chặn số lượng tồn kho bị âm (VD: -5). Kho chỉ có thể chứa từ 0 sản phẩm trở lên.
    // Việc quy về chung 1 message "PRODUCT_VARIANT_STOCK_INVALID" giúp frontend dễ dàng hiển thị 1 thông báo chung ("Tồn kho không hợp lệ") cho mọi trường hợp sai.
    @NotNull(message = "PRODUCT_VARIANT_STOCK_INVALID")
    @PositiveOrZero(message = "PRODUCT_VARIANT_STOCK_INVALID")
    Integer stock;

    // Các trường Enum (VariantStatus, InventoryStatus).
    // Spring Boot mặc định sẽ tự động ánh xạ chuỗi String từ JSON (ví dụ "IN_STOCK")
    // thành giá trị Enum tương ứng. Nếu Frontend gửi sai tên Enum, Spring sẽ quăng lỗi HttpMessageNotReadableException trước khi vào tới Controller.
    VariantStatus active;
    InventoryStatus inventoryStatus;

    // -------------------------------------------------------------------------
    // LIÊN KẾT PHÂN CẤP (FOREIGN KEY)
    // -------------------------------------------------------------------------
    // Trong kiến trúc E-commerce chuẩn: Product -> ProductColor -> ProductVariant.
    // Khi tạo mới một biến thể (Variant), hệ thống bắt buộc phải biết biến thể này
    // nằm trong "Màu sắc" (Color) nào.
    // Nếu Frontend không truyền ID của màu sắc lên, hệ thống sẽ chối từ (throw lỗi).
    @NotNull(message = "PRODUCT_COLOR_NOT_FOUND")
    Long colorId;
}