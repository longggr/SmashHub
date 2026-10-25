package org.example.smashhub.catalog.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.catalog.dto.request.CategoryRequest;
import org.example.smashhub.catalog.dto.response.CategoryResponse;
import org.example.smashhub.catalog.service.CategoryService;
import org.example.smashhub.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryController {

    private static final String CAN_MANAGE = "hasAnyRole('ADMIN','STAFF')";
    private static final String ADMIN_ONLY = "hasRole('ADMIN')";

    CategoryService categoryService;

    // ---------------------------- công khai ----------------------------

    /** Toàn bộ cây danh mục (đã sắp theo displayOrder) - dùng dựng menu. */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getTree() {
        return ApiResponse.<List<CategoryResponse>>builder()
                .result(categoryService.getTree())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getById(@PathVariable Long id) {
        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.getById(id))
                .build();
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<CategoryResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.getBySlug(slug))
                .build();
    }

    @GetMapping("/{id}/children")
    public ApiResponse<List<CategoryResponse>> getChildren(@PathVariable Long id) {
        return ApiResponse.<List<CategoryResponse>>builder()
                .result(categoryService.getChildrenByParentId(id))
                .build();
    }

    // ---------------------------- quản trị ----------------------------

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<CategoryResponse> create(@RequestBody @Valid CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.create(request))
                .message("Category created successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.update(id, request))
                .message("Category updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN_ONLY)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Category deleted successfully")
                .build();
    }
}