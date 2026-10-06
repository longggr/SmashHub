package org.example.smashhub.catalog.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.catalog.dto.request.BrandRequest;
import org.example.smashhub.catalog.dto.response.BrandResponse;
import org.example.smashhub.catalog.entity.Brand;
import org.example.smashhub.catalog.mapper.BrandMapper;
import org.example.smashhub.catalog.repository.BrandRepository;
import org.example.smashhub.catalog.service.BrandService;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BrandServiceImpl implements BrandService {
    BrandRepository brandRepository;
    BrandMapper brandMapper;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "brand", allEntries = true),
            @CacheEvict(cacheNames = "brands",allEntries = true)
    })
    public BrandResponse create(BrandRequest request) {
        if (brandRepository.existsBySlug(request.getSlug()))
            throw new AppException(ErrorCode.BRAND_SLUG_EXISTED);

        Brand brand = brandMapper.toBrand(request);
        return brandMapper.toBrandResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "brand", allEntries = true),
            @CacheEvict(cacheNames = "brands",allEntries = true)
    })
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(id).orElseThrow(()->
                new AppException(ErrorCode.BRAND_NOT_FOUND));
        if (!brand.getSlug().equals(request.getSlug()) && brandRepository.existsBySlug(request.getSlug()))
            throw new AppException(ErrorCode.BRAND_SLUG_EXISTED);
        return brandMapper.toBrandResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "brands", allEntries = true),
            @CacheEvict(cacheNames = "brand", allEntries = true)
    })
    public void delete(Long id) {
        if (!brandRepository.existsById(id))
            throw new AppException(ErrorCode.BRAND_NOT_FOUND);
        brandRepository.deleteById(id);
    }

    @Override
    public BrandResponse getById(Long id) {
        return null;
    }

    @Override
    public BrandResponse getBySlug(String slug) {
        return null;
    }

    @Override
    public List<BrandResponse> getAll() {
        return List.of();
    }
}
