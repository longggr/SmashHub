package org.example.smashhub.catalog.service;

import org.example.smashhub.catalog.dto.request.ProductRequest;
import org.example.smashhub.catalog.dto.request.ProductSearchRequest;
import org.example.smashhub.catalog.dto.response.ProductResponse;
import org.example.smashhub.catalog.dto.response.ProductSummaryResponse;
import org.example.smashhub.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;


public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    /** Xóa mềm: chuyển sang DISCONTINUED. */
    void deleteProduct(Long id);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySlug(String slug);

    PageResponse<ProductSummaryResponse> searchProducts(ProductSearchRequest request, Pageable pageable);
}