package org.example.smashhub.catalog.service;

import org.example.smashhub.catalog.dto.request.BrandRequest;
import org.example.smashhub.catalog.dto.response.BrandResponse;
import org.example.smashhub.common.dto.PageResponse;
import org.example.smashhub.common.enums.ProductType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BrandService {
    BrandResponse create(BrandRequest request);
    BrandResponse update(Long id, BrandRequest request);
    void delete(Long id);
    BrandResponse getById(Long id);
    BrandResponse getBySlug(String slug);
    List<BrandResponse> getAll();
    PageResponse<BrandResponse> searchBrands(String keyword, Pageable pageable);
    BrandResponse updateLogo(Long id, String newLogoUrl);
    List<BrandResponse> getBrandsByCategory(Long categoryId);
    List<BrandResponse> getBrandsByProductType(ProductType type);
}
