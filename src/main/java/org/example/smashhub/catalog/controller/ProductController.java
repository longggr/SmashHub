package org.example.smashhub.catalog.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.catalog.dto.request.*;
import org.example.smashhub.catalog.dto.response.*;
import org.example.smashhub.catalog.service.InventoryService;
import org.example.smashhub.catalog.service.ProductAttributeService;
import org.example.smashhub.catalog.service.ProductMediaService;
import org.example.smashhub.catalog.service.ProductService;
import org.example.smashhub.common.dto.PageResponse;
import org.example.smashhub.common.enums.ProductStatus;
import org.example.smashhub.common.enums.ProductType;
import org.example.smashhub.common.response.ApiResponse;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {

    // Quyền: ADMIN và STAFF quản lý catalog, riêng thao tác xóa chỉ ADMIN.
    private static final String CAN_MANAGE = "hasAnyRole('ADMIN','STAFF')";
    private static final String ADMIN_ONLY = "hasRole('ADMIN')";

    // Chỉ cho sort theo các field này (client không được truyền tên field bất kỳ vào Sort).
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "name", "price", "salePrice", "createDate", "updateDate");
    private static final int MAX_PAGE_SIZE = 50;

    ProductService productService;
    ProductAttributeService attributeService;
    ProductMediaService mediaService;
    InventoryService inventoryService;

    // ============================== PRODUCT ==============================

    /**
     * Tìm kiếm / lọc / phân trang (công khai).
     * attr: lọc theo thuộc tính EAV, dạng attr=tênThuộcTính:giáTrị, truyền nhiều lần được.
     *   vd /products?attr=weight:4U&attr=grip_size:G5
     * page bắt đầu từ 0.
     */
    @GetMapping
    public ApiResponse<PageResponse<ProductSummaryResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(required = false) List<String> attr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {

        ProductSearchRequest request = ProductSearchRequest.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .brandId(brandId)
                .type(type)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .onSale(onSale)
                .attributes(parseAttributeFilters(attr))
                // Khách thường chỉ thấy ACTIVE; ADMIN/STAFF mới được lọc theo status bất kỳ (xem DRAFT, DISCONTINUED...).
                .status(isStaff() ? status : ProductStatus.ACTIVE)
                .build();

        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createDate";
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(sortDirection, safeSortBy));

        return ApiResponse.<PageResponse<ProductSummaryResponse>>builder()
                .result(productService.searchProducts(request, pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.<ProductResponse>builder()
                .result(hideIfNotPublic(productService.getProductById(id)))
                .build();
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<ProductResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.<ProductResponse>builder()
                .result(hideIfNotPublic(productService.getProductBySlug(slug)))
                .build();
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ProductResponse> create(@RequestBody @Valid ProductRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.createProduct(request))
                .message("Product created successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ProductResponse> update(@PathVariable Long id,
                                               @RequestBody @Valid ProductRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.updateProduct(id, request))
                .message("Product updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN_ONLY)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.<Void>builder()
                .message("Product has been discontinued")
                .build();
    }

    // ============================== ATTRIBUTE ==============================

    @PostMapping("/{productId}/attributes")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ProductAttributeResponse> addAttribute(@PathVariable Long productId,
                                                              @RequestBody @Valid ProductAttributeRequest request) {
        return ApiResponse.<ProductAttributeResponse>builder()
                .result(attributeService.addAttribute(productId, request))
                .build();
    }

    @PutMapping("/{productId}/attributes/{attributeId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ProductAttributeResponse> updateAttribute(@PathVariable Long productId,
                                                                 @PathVariable Long attributeId,
                                                                 @RequestBody @Valid ProductAttributeRequest request) {
        return ApiResponse.<ProductAttributeResponse>builder()
                .result(attributeService.updateAttribute(productId, attributeId, request))
                .build();
    }

    @DeleteMapping("/{productId}/attributes/{attributeId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteAttribute(@PathVariable Long productId, @PathVariable Long attributeId) {
        attributeService.deleteAttribute(productId, attributeId);
        return ApiResponse.<Void>builder().message("Attribute deleted").build();
    }

    // ============================== COLOR ==============================

    @PostMapping("/{productId}/colors")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ProductColorResponse> addColor(@PathVariable Long productId,
                                                      @RequestBody @Valid ProductColorRequest request) {
        return ApiResponse.<ProductColorResponse>builder()
                .result(mediaService.addColor(productId, request))
                .build();
    }

    @PutMapping("/{productId}/colors/{colorId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ProductColorResponse> updateColor(@PathVariable Long productId,
                                                         @PathVariable Long colorId,
                                                         @RequestBody @Valid ProductColorRequest request) {
        return ApiResponse.<ProductColorResponse>builder()
                .result(mediaService.updateColor(productId, colorId, request))
                .build();
    }

    @DeleteMapping("/{productId}/colors/{colorId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteColor(@PathVariable Long productId, @PathVariable Long colorId) {
        mediaService.deleteColor(productId, colorId);
        return ApiResponse.<Void>builder().message("Color deleted").build();
    }

    // ============================== IMAGE ==============================
    // Upload file lên Cloudinary qua POST /files/upload trước, rồi gửi url + publicId nhận được vào đây.

    @PostMapping("/{productId}/images")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ProductImageResponse> addImage(@PathVariable Long productId,
                                                      @RequestBody @Valid ProductImageRequest request) {
        return ApiResponse.<ProductImageResponse>builder()
                .result(mediaService.addImage(productId, request))
                .build();
    }

    @PutMapping("/{productId}/images/{imageId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ProductImageResponse> updateImage(@PathVariable Long productId,
                                                         @PathVariable Long imageId,
                                                         @RequestBody @Valid ProductImageRequest request) {
        return ApiResponse.<ProductImageResponse>builder()
                .result(mediaService.updateImage(productId, imageId, request))
                .build();
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteImage(@PathVariable Long productId, @PathVariable Long imageId) {
        mediaService.deleteImage(productId, imageId);
        return ApiResponse.<Void>builder().message("Image deleted").build();
    }

    // ============================== VARIANT ==============================
    // deductStock / restoreStock cố ý KHÔNG có endpoint: chỉ OrderService gọi nội bộ.

    @PostMapping("/{productId}/variants")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ProductVariantResponse> addVariant(@PathVariable Long productId,
                                                          @RequestBody @Valid ProductVariantRequest request) {
        return ApiResponse.<ProductVariantResponse>builder()
                .result(inventoryService.addVariant(productId, request))
                .build();
    }

    @PutMapping("/{productId}/variants/{variantId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ProductVariantResponse> updateVariant(@PathVariable Long productId,
                                                             @PathVariable Long variantId,
                                                             @RequestBody @Valid ProductVariantRequest request) {
        return ApiResponse.<ProductVariantResponse>builder()
                .result(inventoryService.updateVariant(productId, variantId, request))
                .build();
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteVariant(@PathVariable Long productId, @PathVariable Long variantId) {
        inventoryService.deleteVariant(productId, variantId);
        return ApiResponse.<Void>builder().message("Variant deactivated").build();
    }

    // ============================== HELPERS ==============================

    /** Người đang gọi có phải ADMIN/STAFF không (anonymous -> false). */
    private boolean isStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_STAFF".equals(a));
    }

    /**
     * Sản phẩm DRAFT/DISCONTINUED/OUT_OF_STOCK không lộ ra khách: trả 404 như thể không tồn tại.
     * Làm ở controller vì service có @Cacheable dùng chung cho cả admin lẫn khách.
     */
    private ProductResponse hideIfNotPublic(ProductResponse product) {
        if (!isStaff() && product.getProductStatus() != ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    /** "weight:4U" -> {attrName=weight, attrValue=4U}. Phần tử sai định dạng bị bỏ qua. */
    private List<ProductAttributeFilter> parseAttributeFilters(List<String> raw) {
        List<ProductAttributeFilter> filters = new ArrayList<>();
        if (raw == null) {
            return filters;
        }
        for (String item : raw) {
            int idx = item == null ? -1 : item.indexOf(':');
            if (idx > 0 && idx < item.length() - 1) {
                filters.add(ProductAttributeFilter.builder()
                        .attrName(item.substring(0, idx))
                        .attrValue(item.substring(idx + 1))
                        .build());
            }
        }
        return filters;
    }
}