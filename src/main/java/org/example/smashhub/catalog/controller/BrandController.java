package org.example.smashhub.catalog.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.catalog.dto.request.BrandRequest;
import org.example.smashhub.catalog.dto.response.BrandResponse;
import org.example.smashhub.catalog.service.BrandService;
import org.example.smashhub.common.dto.PageResponse;
import org.example.smashhub.common.enums.ProductType;
import org.example.smashhub.common.response.ApiResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BrandController {

    private static final String CAN_MANAGE = "hasAnyRole('ADMIN','STAFF')";
    private static final String ADMIN_ONLY = "hasRole('ADMIN')";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "slug", "createDate");
    private static final int MAX_PAGE_SIZE = 50;

    BrandService brandService;

    // ---------------------------- công khai ----------------------------

    @GetMapping
    public ApiResponse<List<BrandResponse>> getAll() {
        return ApiResponse.<List<BrandResponse>>builder()
                .result(brandService.getAll())
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<BrandResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection) {

        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "name";
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(sortDirection, safeSortBy));

        return ApiResponse.<PageResponse<BrandResponse>>builder()
                .result(brandService.searchBrands(keyword, pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<BrandResponse> getById(@PathVariable Long id) {
        return ApiResponse.<BrandResponse>builder()
                .result(brandService.getById(id))
                .build();
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<BrandResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.<BrandResponse>builder()
                .result(brandService.getBySlug(slug))
                .build();
    }

    /** Các thương hiệu đang có sản phẩm ACTIVE trong danh mục (gồm cả danh mục con) - dùng cho bộ lọc. */
    @GetMapping("/by-category/{categoryId}")
    public ApiResponse<List<BrandResponse>> getByCategory(@PathVariable Long categoryId) {
        return ApiResponse.<List<BrandResponse>>builder()
                .result(brandService.getBrandsByCategory(categoryId))
                .build();
    }

    @GetMapping("/by-type/{type}")
    public ApiResponse<List<BrandResponse>> getByType(@PathVariable ProductType type) {
        return ApiResponse.<List<BrandResponse>>builder()
                .result(brandService.getBrandsByProductType(type))
                .build();
    }

    // ---------------------------- quản trị ----------------------------

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<BrandResponse> create(@RequestBody @Valid BrandRequest request) {
        return ApiResponse.<BrandResponse>builder()
                .result(brandService.create(request))
                .message("Brand created successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<BrandResponse> update(@PathVariable Long id, @RequestBody @Valid BrandRequest request) {
        return ApiResponse.<BrandResponse>builder()
                .result(brandService.update(id, request))
                .message("Brand updated successfully")
                .build();
    }

    /** Đổi logo (logo cũ trên Cloudinary được xóa). Upload file trước qua POST /files/upload để lấy url. */
    @PatchMapping("/{id}/logo")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<BrandResponse> updateLogo(@PathVariable Long id, @RequestParam("logoUrl") String logoUrl) {
        return ApiResponse.<BrandResponse>builder()
                .result(brandService.updateLogo(id, logoUrl))
                .message("Brand logo updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN_ONLY)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Brand deleted successfully")
                .build();
    }
}