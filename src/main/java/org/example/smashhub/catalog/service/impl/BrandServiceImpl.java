package org.example.smashhub.catalog.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
import org.example.smashhub.file.dto.FileUploadResponse;
import org.example.smashhub.file.enums.FileFolder;
import org.example.smashhub.file.service.FileService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
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
    public BrandResponse create(BrandRequest request, MultipartFile logo) {
        String name = request.getName().trim();
        String slug = request.getSlug().trim().toLowerCase();

        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(ErrorCode.BRAND_NAME_EXISTED);
        }
        if (brandRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.BRAND_SLUG_EXISTED);
        }

        Brand brand = brandMapper.toBrand(request);
        brand.setName(name);
        brand.setSlug(slug);
        // logoUrl từ request bị BrandMapper map; đặt lại null để tránh nhận URL tự do từ client
        // (logo chỉ được gán qua file upload an toàn bên dưới).
        brand.setLogoUrl(null);
        brand.setLogoPublicId(null);

        // Upload SAU khi đã qua kiểm tra nghiệp vụ: request sai không sinh file rác.
        if (fileService.isPresent(logo)) {
            FileUploadResponse uploaded = fileService.uploadInTransaction(logo, FileFolder.BRAND);
            brand.setLogoUrl(uploaded.getUrl());
            brand.setLogoPublicId(uploaded.getPublicId());
        }

        BrandResponse response = brandMapper.toBrandResponse(saveBrand(brand));
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
    public BrandResponse update(Long id, BrandRequest request, MultipartFile logo) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        String name = request.getName().trim();
        String slug = request.getSlug().trim().toLowerCase();

        if (!brand.getName().equalsIgnoreCase(name) && brandRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(ErrorCode.BRAND_NAME_EXISTED);
        }
        if (!brand.getSlug().equalsIgnoreCase(slug) && brandRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.BRAND_SLUG_EXISTED);
        }

        // Lưu lại publicId cũ trước khi mapper ghi đè (mapper IGNORE null, nên logoUrl trong
        // request không làm thay đổi logo nếu không có file).
        String oldPublicId = brand.getLogoPublicId();

        brandMapper.updateBrand(brand, request);
        brand.setName(name);
        brand.setSlug(slug);

        if (fileService.isPresent(logo)) {
            // Có file mới: upload → gán url + publicId mới.
            FileUploadResponse uploaded = fileService.uploadInTransaction(logo, FileFolder.BRAND);
            brand.setLogoUrl(uploaded.getUrl());
            brand.setLogoPublicId(uploaded.getPublicId());
        }
        // Nếu không có file mới: giữ nguyên logoUrl + logoPublicId cũ (mapper đã IGNORE null).

        // Nếu publicId thực sự thay đổi → xóa ảnh cũ SAU KHI transaction commit.
        deleteOldLogoAfterCommit(oldPublicId, brand.getLogoPublicId());

        BrandResponse response = brandMapper.toBrandResponse(saveBrand(brand));
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

        String logoPublicId = brand.getLogoPublicId();
        brandRepository.delete(brand);

        // Xóa ảnh trên Cloudinary SAU KHI DB commit; nếu không có publicId thì bỏ qua.
        if (logoPublicId != null && !logoPublicId.isBlank()) {
            fileService.deleteAfterCommit(logoPublicId);
        }
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

    /**
     * Cập nhật logo bằng URL thuần (không qua file upload) — chỉ dùng trong các trường hợp
     * migrate dữ liệu cũ hoặc quản trị viên biết chắc URL Cloudinary hợp lệ.
     * Không xóa ảnh cũ trên Cloudinary vì không có publicId cũ đáng tin cậy.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "brand", allEntries = true),
            @CacheEvict(cacheNames = "brands", allEntries = true)
    })
    public BrandResponse updateLogo(Long id, String newLogoUrl) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        brand.setLogoUrl(newLogoUrl);
        // Không clear logoPublicId vì URL thuần không kèm publicId mới;
        // để publicId cũ tránh mất thông tin quản lý.

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

    // ------------------------------ helpers ------------------------------

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

    /** Race giữa 2 request cùng tạo/sửa slug: unique constraint DB chặn lần ghi thứ hai. */
    private Brand saveBrand(Brand brand) {
        try {
            return brandRepository.saveAndFlush(brand);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.BRAND_SLUG_EXISTED);
        }
    }

    /**
     * Nếu publicId logo thực sự thay đổi → lên lịch xóa ảnh cũ SAU KHI commit.
     * So sánh theo publicId (không theo URL) để tránh phụ thuộc vào format URL Cloudinary.
     */
    private void deleteOldLogoAfterCommit(String oldPublicId, String newPublicId) {
        if (oldPublicId != null && !oldPublicId.isBlank() && !oldPublicId.equals(newPublicId)) {
            fileService.deleteAfterCommit(oldPublicId);
        }
    }
}