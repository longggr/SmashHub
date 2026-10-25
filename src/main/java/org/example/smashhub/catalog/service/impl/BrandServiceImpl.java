package org.example.smashhub.catalog.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.catalog.dto.request.BrandRequest;
import org.example.smashhub.catalog.dto.response.BrandResponse;
import org.example.smashhub.catalog.entity.Brand;
import org.example.smashhub.catalog.mapper.BrandMapper;
import org.example.smashhub.catalog.repository.BrandRepository;
import org.example.smashhub.catalog.repository.ProductRepository;
import org.example.smashhub.catalog.service.BrandService;
import org.example.smashhub.catalog.service.CategoryService;
import org.example.smashhub.common.dto.PageResponse;
import org.example.smashhub.common.enums.ProductStatus;
import org.example.smashhub.common.enums.ProductType;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.example.smashhub.file.service.FileService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BrandServiceImpl implements BrandService {

    BrandRepository brandRepository;
    ProductRepository productRepository;
    CategoryService categoryService;
    BrandMapper brandMapper;
    FileService fileService;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "brand", allEntries = true),
            @CacheEvict(cacheNames = "brands", allEntries = true)
    })
    public BrandResponse create(BrandRequest request) {
        if (brandRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new AppException(ErrorCode.BRAND_NAME_EXISTED);
        }
        if (brandRepository.existsBySlug(request.getSlug().trim())) {
            throw new AppException(ErrorCode.BRAND_SLUG_EXISTED);
        }

        Brand brand = brandMapper.toBrand(request);
        brand.setName(request.getName().trim());
        brand.setSlug(request.getSlug().trim().toLowerCase());

        BrandResponse response = brandMapper.toBrandResponse(brandRepository.save(brand));
        response.setProductCount(0L);
        return response;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "brand", allEntries = true),
            @CacheEvict(cacheNames = "brands", allEntries = true),
            @CacheEvict(cacheNames = "product", allEntries = true),
            @CacheEvict(cacheNames = "productBySlug", allEntries = true)
    })
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        if (!brand.getName().equalsIgnoreCase(request.getName().trim())
                && brandRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new AppException(ErrorCode.BRAND_NAME_EXISTED);
        }

        if (!brand.getSlug().equalsIgnoreCase(request.getSlug().trim())
                && brandRepository.existsBySlug(request.getSlug().trim())) {
            throw new AppException(ErrorCode.BRAND_SLUG_EXISTED);
        }

        String oldLogoUrl = brand.getLogoUrl();
        if (request.getLogoUrl() != null && oldLogoUrl != null && !oldLogoUrl.equals(request.getLogoUrl())) {
            deleteCloudinaryLogoSafely(oldLogoUrl);
        }

        brandMapper.updateBrand(brand, request);
        brand.setName(request.getName().trim());
        brand.setSlug(request.getSlug().trim().toLowerCase());

        BrandResponse response = brandMapper.toBrandResponse(brandRepository.save(brand));
        response.setProductCount(productRepository.countByBrandIdAndProductStatus(id, ProductStatus.ACTIVE));
        return response;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "brands", allEntries = true),
            @CacheEvict(cacheNames = "brand", allEntries = true)
    })
    public void delete(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        if (productRepository.existsByBrandId(id)) {
            throw new AppException(ErrorCode.BRAND_HAS_PRODUCTS);
        }

        if (brand.getLogoUrl() != null && !brand.getLogoUrl().isBlank()) {
            deleteCloudinaryLogoSafely(brand.getLogoUrl());
        }

        brandRepository.delete(brand);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "brand", key = "'id:' + #id")
    public BrandResponse getById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
        BrandResponse response = brandMapper.toBrandResponse(brand);
        response.setProductCount(productRepository.countByBrandIdAndProductStatus(id, ProductStatus.ACTIVE));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "brand", key = "'slug:' + #slug")
    public BrandResponse getBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
        BrandResponse response = brandMapper.toBrandResponse(brand);
        response.setProductCount(productRepository.countByBrandIdAndProductStatus(brand.getId(), ProductStatus.ACTIVE));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "brands", key = "'all'")
    public List<BrandResponse> getAll() {
        List<Brand> brands = brandRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return attachProductCounts(brands);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> searchBrands(String keyword, Pageable pageable) {
        Page<Brand> page = brandRepository.searchBrands(keyword, pageable);
        List<BrandResponse> content = attachProductCounts(page.getContent());

        return PageResponse.<BrandResponse>builder()
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .content(content)
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "brand", allEntries = true),
            @CacheEvict(cacheNames = "brands", allEntries = true)
    })
    public BrandResponse updateLogo(Long id, String newLogoUrl) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        if (brand.getLogoUrl() != null && !brand.getLogoUrl().equals(newLogoUrl)) {
            deleteCloudinaryLogoSafely(brand.getLogoUrl());
        }

        brand.setLogoUrl(newLogoUrl);
        BrandResponse response = brandMapper.toBrandResponse(brandRepository.save(brand));
        response.setProductCount(productRepository.countByBrandIdAndProductStatus(id, ProductStatus.ACTIVE));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "brands", key = "'category:' + #categoryId")
    public List<BrandResponse> getBrandsByCategory(Long categoryId) {
        List<Long> categoryIds = categoryService.getSelfAndDescendantIds(categoryId);
        List<Brand> brands = brandRepository.findActiveBrandsByCategoryIds(categoryIds, ProductStatus.ACTIVE);
        return attachProductCounts(brands);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "brands", key = "'type:' + #type.name()")
    public List<BrandResponse> getBrandsByProductType(ProductType type) {
        List<Brand> brands = brandRepository.findActiveBrandsByProductType(type, ProductStatus.ACTIVE);
        return attachProductCounts(brands);
    }

    private List<BrandResponse> attachProductCounts(List<Brand> brands) {
        if (brands.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> countMap = productRepository.countProductsGroupByBrand(ProductStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return brands.stream()
                .map(brand -> {
                    BrandResponse res = brandMapper.toBrandResponse(brand);
                    res.setProductCount(countMap.getOrDefault(brand.getId(), 0L));
                    return res;
                })
                .toList();
    }

    private void deleteCloudinaryLogoSafely(String logoUrl) {
        try {
            if (logoUrl.contains("/upload/")) {
                String afterUpload = logoUrl.substring(logoUrl.indexOf("/upload/") + 8);
                if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
                    afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
                }
                int dotIndex = afterUpload.lastIndexOf('.');
                String publicId = (dotIndex != -1) ? afterUpload.substring(0, dotIndex) : afterUpload;
                fileService.delete(publicId);
            }
        } catch (Exception e) {
            log.warn("Could not delete old brand logo from Cloudinary: {}", logoUrl, e);
        }
    }
}