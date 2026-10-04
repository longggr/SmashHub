package org.example.smashhub.catalog.mapper;

import org.example.smashhub.catalog.dto.request.ProductRequest;
import org.example.smashhub.catalog.dto.response.*;
import org.example.smashhub.catalog.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Cấu hình tổng quan cho Mapper:
 * - componentModel = "spring": Biến Mapper này thành một Spring Bean (@Component),
 *   cho phép bạn inject nó ở tầng Service bằng @Autowired hoặc constructor injection.
 * - nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE:
 *   Khi cập nhật dữ liệu, nếu một trường trong DTO gửi lên có giá trị null, MapStruct sẽ
 *   BỎ QUA và giữ nguyên giá trị cũ của Entity thay vì ghi đè bằng null (rất hữu ích cho API PATCH / partial update).
 * - uses = {BrandMapper.class, CategoryMapper.class}:
 *   Khai báo các Mapper phụ trợ. Khi ánh xạ các đối tượng lồng nhau như Brand -> BrandResponse
 *   hoặc Category -> CategoryResponse, MapStruct sẽ tái sử dụng logic từ 2 mapper này.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {BrandMapper.class, CategoryMapper.class})
public interface ProductMapper {

    /**
     * Chuyển đổi dữ liệu tạo mới từ DTO (ProductRequest) sang Entity (Product).
     * - target = "category", ignore = true: Bỏ qua trường category. Lý do: request thường chỉ
     *   gửi categoryId (Long/UUID/String), tầng Service sẽ cần query Category từ DB lên rồi tự set vào Entity.
     * - target = "brand", ignore = true: Tương tự như trên, Service sẽ tự truy vấn Brand theo brandId và gán vào sau.
     */
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    Product toProduct(ProductRequest request);

    /**
     * Cập nhật thông tin của một Product Entity đã tồn tại từ ProductRequest.
     * - @MappingTarget: Báo cho MapStruct biết 'product' là đối tượng ĐÍCH sẽ được cập nhật trực tiếp,
     *   không phải tạo mới đối tượng.
     * - Vẫn bỏ qua category và brand để Service tự xử lý quan hệ foreign key khi cập nhật.
     * - Nhờ nullValuePropertyMappingStrategy.IGNORE ở trên, trường nào trong request bị null sẽ không làm mất dữ liệu cũ của product.
     */
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    void updateProduct(@MappingTarget Product product, ProductRequest request);

    /**
     * Chuyển đổi chi tiết một Product Entity sang DTO trả về cho Client (ProductResponse).
     * - target = "brand", source = "brand": Dùng BrandMapper (khai báo trong uses) để map entity Brand sang BrandResponse.
     * - target = "category", source = "category": Tương tự, dùng CategoryMapper để map sang CategoryResponse.
     * - target = "attributes", ignore = true: Bỏ qua danh sách thuộc tính (VD: kích thước, trọng lượng...),
     *   thường do Service map thủ công hoặc lazy loading để tối ưu query.
     * - target = "colors", ignore = true: Bỏ qua danh sách màu sắc/biến thể liên quan để xử lý riêng biệt.
     */
    @Mapping(target = "brand", source = "brand")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "attributes", ignore = true)
    @Mapping(target = "colors", ignore = true)
    ProductResponse toProductResponse(Product product);

    /**
     * Chuyển đổi Product Entity sang ProductSummaryResponse (bản tóm tắt nhẹ, thường dùng cho trang danh sách/tìm kiếm).
     * - source = "category.name": Trích xuất trực tiếp tên danh mục từ object lồng nhau (product.getCategory().getName())
     *   gán vào thuộc tính categoryName của DTO.
     * - source = "brand.name": Tương tự, lấy product.getBrand().getName() gán vào brandName.
     */
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandName", source = "brand.name")
    ProductSummaryResponse toProductSummaryResponse(Product product);

    /**
     * Ánh xạ danh sách tóm tắt sản phẩm.
     * MapStruct sẽ tự động duyệt vòng lặp List và gọi toProductSummaryResponse(Product) cho từng phần tử.
     */
    List<ProductSummaryResponse> toProductSummaryResponseList(List<Product> products);

    // ==========================================
    // CÁC HÀM MAP CHO CÁC ENTITY CON / LIÊN KẾT
    // ==========================================

    /** Map đơn và map danh sách cho thuộc tính động của sản phẩm (ví dụ: chất liệu, kích thước...) */
    ProductAttributeResponse toProductAttributeResponse(ProductAttribute attribute);
    List<ProductAttributeResponse> toProductAttributeResponseList(List<ProductAttribute> attributes);

    /** Map đơn và map danh sách cho hình ảnh của sản phẩm */
    ProductImageResponse toProductImageResponse(ProductImage image);
    List<ProductImageResponse> toProductImageResponseList(List<ProductImage> images);

    /** Map đơn và map danh sách cho biến thể sản phẩm (SKU, giá bán riêng, tồn kho riêng...) */
    ProductVariantResponse toProductVariantResponse(ProductVariant variant);
    List<ProductVariantResponse> toProductVariantResponseList(List<ProductVariant> variants);

    /**
     * Map thông tin nhóm màu sắc của sản phẩm sang ProductColorResponse.
     * - target = "images", ignore = true: Bỏ qua danh sách ảnh của màu này.
     * - target = "variants", ignore = true: Bỏ qua danh sách biến thể con của màu này.
     * Cả 2 được ignore nhằm tránh vòng lặp quan hệ vô tận (circular reference) hoặc N+1 query khi lazy-load.
     */
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "variants", ignore = true)
    ProductColorResponse toProductColorResponse(ProductColor color);
}