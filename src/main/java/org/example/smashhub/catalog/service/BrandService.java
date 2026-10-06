package org.example.smashhub.catalog.service;

import org.example.smashhub.catalog.dto.request.BrandRequest;
import org.example.smashhub.catalog.dto.response.BrandResponse;

import java.util.List;

public interface BrandService {
    BrandResponse create(BrandRequest request);
    BrandResponse update(Long id, BrandRequest request);
    void delete(Long id);
    BrandResponse getById(Long id);
    BrandResponse getBySlug(String slug);
    List<BrandResponse> getAll();
}
