package org.example.smashhub.catalog.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.catalog.cache.CatalogCacheEvictor;
import org.example.smashhub.catalog.dto.request.ProductRequest;
import org.example.smashhub.catalog.dto.request.ProductSearchRequest;
import org.example.smashhub.catalog.dto.response.*;
import org.example.smashhub.catalog.entity.*;
import org.example.smashhub.catalog.mapper.ProductMapper;
import org.example.smashhub.catalog.repository.*;
import org.example.smashhub.catalog.service.CategoryService;
import org.example.smashhub.catalog.service.ProductService;
import org.example.smashhub.catalog.specification.ProductSpecification;
import org.example.smashhub.common.dto.PageResponse;
import org.example.smashhub.common.enums.ImagePriority;
import org.example.smashhub.common.enums.ProductStatus;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phần SPU của catalog. Phần con (attribute/color/image/variant) nằm ở các service riêng,
 * ở đây chỉ ĐỌC chúng để ghép thành ProductResponse đầy đủ.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductServiceImpl implements ProductService {

    ProductRepository productRepository;
    CategoryRepository categoryRepository;
    BrandRepository brandRepository;
    ProductAttributeRepository attributeRepository;
    ProductColorRepository colorRepository;
    ProductImageRepository imageRepository;
    ProductVariantRepository variantRepository;

    CategoryService categoryService;
    ProductMapper productMapper;
    CatalogCacheEvictor cacheEvictor;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // Chuẩn hóa slug TRƯỚC khi kiểm tra trùng (trước đây check bản chưa lowercase nhưng lưu bản lowercase).
        String slug = normalizeSlug(request.getSlug());
        if (productRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTED);
        }
        validatePrices(request.getPrice(), request.getSalePrice());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        Product product = productMapper.toProduct(request);
        product.setName(request.getName().trim());
        product.setSlug(slug);
        product.setCategory(category);
        product.setBrand(brand);

        Product saved = saveProduct(product);

        // Cache brand chứa productCount nên phải làm mới.
        cacheEvictor.evictBrands();
        return assembleFullProductResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        String slug = normalizeSlug(request.getSlug());
        if (!product.getSlug().equals(slug) && productRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTED);
        }
        validatePrices(request.getPrice(), request.getSalePrice());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        productMapper.updateProduct(product, request);
        product.setName(request.getName().trim());
        product.setSlug(slug);
        product.setCategory(category);
        product.setBrand(brand);
        // Mapper dùng NullValuePropertyMappingStrategy.IGNORE nên gửi salePrice = null sẽ KHÔNG xóa được giá khuyến mãi.
        // Gán thủ công để null nghĩa là "bỏ khuyến mãi".
        product.setSalePrice(request.getSalePrice());

        Product updated = saveProduct(product);

        cacheEvictor.evictProduct(id);   // đã xóa luôn toàn bộ cache theo slug (kể cả slug cũ)
        cacheEvictor.evictBrands();
        return assembleFullProductResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        product.setProductStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);

        cacheEvictor.evictProduct(id);
        cacheEvictor.evictBrands();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "product", key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return assembleFullProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    // key lowercase để "ABC" và "abc" dùng chung một entry cache.
    @Cacheable(cacheNames = "productBySlug", key = "#slug.trim().toLowerCase()")
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(normalizeSlug(slug))
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return assembleFullProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> searchProducts(ProductSearchRequest request, Pageable pageable) {
        List<Long> targetCategoryIds = null;
        if (request.getCategoryId() != null) {
            // Lọc theo danh mục cha thì gồm cả toàn bộ danh mục con/cháu.
            targetCategoryIds = categoryService.getSelfAndDescendantIds(request.getCategoryId());
        }

        Specification<Product> spec = ProductSpecification.build(
                request.getKeyword(),
                targetCategoryIds,
                request.getBrandId(),
                request.getType(),
                request.getStatus(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getOnSale(),
                request.getAttributes()
        );

        Page<Product> page = productRepository.findAll(spec, pageable);

        // 1 query lấy thumbnail cho cả trang (thay vì 1 query / sản phẩm).
        Map<Long, String> thumbnails = resolveThumbnails(
                page.getContent().stream().map(Product::getId).collect(Collectors.toList()));

        List<ProductSummaryResponse> content = page.getContent().stream()
                .map(product -> {
                    ProductSummaryResponse summary = productMapper.toProductSummaryResponse(product);
                    summary.setThumbnailUrl(thumbnails.get(product.getId()));
                    return summary;
                })
                .collect(Collectors.toList());

        return PageResponse.<ProductSummaryResponse>builder()
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .content(content)
                .build();
    }

    // ------------------------------ helpers ------------------------------

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase();
    }

    /** Giá khuyến mãi (nếu có) phải > 0 và nhỏ hơn giá gốc. */
    private void validatePrices(BigDecimal price, BigDecimal salePrice) {
        if (salePrice != null && (salePrice.signum() <= 0 || salePrice.compareTo(price) >= 0)) {
            throw new AppException(ErrorCode.PRODUCT_SALE_PRICE_INVALID);
        }
    }

    /** Race giữa 2 request cùng tạo 1 slug: check existsBySlug đều qua, unique constraint của DB chặn lần ghi thứ hai. */
    private Product saveProduct(Product product) {
        try {
            return productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTED);
        }
    }

    /** Thumbnail cho nhiều sản phẩm từ 1 query: ưu tiên ảnh MAIN đầu tiên, không có thì ảnh đầu tiên. */
    private Map<Long, String> resolveThumbnails(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>();
        Set<Long> hasMain = new HashSet<>();

        for (Object[] row : imageRepository.findThumbnailCandidates(productIds)) {
            Long productId = (Long) row[0];
            String url = (String) row[1];
            ImagePriority priority = (ImagePriority) row[2];

            if (priority == ImagePriority.MAIN) {
                if (hasMain.add(productId)) {
                    result.put(productId, url); // ghi đè ảnh thường đã chọn tạm trước đó
                }
            } else {
                result.putIfAbsent(productId, url);
            }
        }
        return result;
    }

    private ProductResponse assembleFullProductResponse(Product product) {
        Long productId = product.getId();
        ProductResponse response = productMapper.toProductResponse(product);

        List<ProductAttribute> attributes = attributeRepository.findByProductId(productId);
        response.setAttributes(new ArrayList<>(productMapper.toProductAttributeResponseList(attributes)));

        List<ProductColor> colors = colorRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        if (colors.isEmpty()) {
            response.setColors(new ArrayList<>());
            return response;
        }

        List<ProductImage> allImages = imageRepository.findByColorProductIdOrderByOrderIndexAsc(productId);
        List<ProductVariant> allVariants = variantRepository.findByColorProductId(productId);

        Map<Long, List<ProductImageResponse>> imagesByColorId = allImages.stream()
                .collect(Collectors.groupingBy(
                        img -> img.getColor().getId(),
                        Collectors.mapping(productMapper::toProductImageResponse, Collectors.toList())
                ));

        Map<Long, List<ProductVariantResponse>> variantsByColorId = allVariants.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getColor().getId(),
                        Collectors.mapping(productMapper::toProductVariantResponse, Collectors.toList())
                ));

        // Dùng ArrayList (không dùng Collections.emptyList()/Stream.toList()) cho các list nằm trong object được cache Redis
        // để Jackson default-typing serialize/deserialize không vướng các class List bất biến đặc biệt của JDK.
        List<ProductColorResponse> colorResponses = colors.stream()
                .map(color -> {
                    ProductColorResponse colorRes = productMapper.toProductColorResponse(color);
                    colorRes.setImages(imagesByColorId.getOrDefault(color.getId(), new ArrayList<>()));
                    colorRes.setVariants(variantsByColorId.getOrDefault(color.getId(), new ArrayList<>()));
                    return colorRes;
                })
                .collect(Collectors.toList());

        response.setColors(colorResponses);
        return response;
    }
}