package org.example.smashhub.catalog.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.catalog.cache.CatalogCacheEvictor;
import org.example.smashhub.catalog.dto.request.ProductColorRequest;
import org.example.smashhub.catalog.dto.request.ProductImageRequest;
import org.example.smashhub.catalog.dto.response.ProductColorResponse;
import org.example.smashhub.catalog.dto.response.ProductImageResponse;
import org.example.smashhub.catalog.entity.Product;
import org.example.smashhub.catalog.entity.ProductColor;
import org.example.smashhub.catalog.entity.ProductImage;
import org.example.smashhub.catalog.mapper.ProductMapper;
import org.example.smashhub.catalog.repository.ProductColorRepository;
import org.example.smashhub.catalog.repository.ProductImageRepository;
import org.example.smashhub.catalog.repository.ProductRepository;
import org.example.smashhub.catalog.repository.ProductVariantRepository;
import org.example.smashhub.catalog.service.ProductMediaService;
import org.example.smashhub.common.enums.ImagePriority;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.example.smashhub.file.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductMediaServiceImpl implements ProductMediaService {

    ProductRepository productRepository;
    ProductColorRepository colorRepository;
    ProductImageRepository imageRepository;
    ProductVariantRepository variantRepository;
    ProductMapper productMapper;
    CatalogCacheEvictor cacheEvictor;
    FileService fileService;

    // =========================================================================
    // COLOR
    // =========================================================================

    @Override
    @Transactional
    public ProductColorResponse addColor(Long productId, ProductColorRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        String colorName = request.getColorName().trim();
        if (colorRepository.existsByProductIdAndColorNameIgnoreCase(productId, colorName)) {
            throw new AppException(ErrorCode.PRODUCT_COLOR_NAME_EXISTED);
        }

        ProductColor color = ProductColor.builder()
                .product(product)
                .colorName(colorName)
                .hexCode(request.getHexCode())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        ProductColor saved = colorRepository.save(color);
        cacheEvictor.evictProduct(productId);
        return productMapper.toProductColorResponse(saved);
    }

    @Override
    @Transactional
    public ProductColorResponse updateColor(Long productId, Long colorId, ProductColorRequest request) {
        ProductColor color = requireColor(productId, colorId);

        String colorName = request.getColorName().trim();
        if (colorRepository.existsByProductIdAndColorNameIgnoreCaseAndIdNot(productId, colorName, colorId)) {
            throw new AppException(ErrorCode.PRODUCT_COLOR_NAME_EXISTED);
        }

        color.setColorName(colorName);
        if (request.getHexCode() != null) {
            color.setHexCode(request.getHexCode());
        }
        if (request.getDisplayOrder() != null) {
            color.setDisplayOrder(request.getDisplayOrder());
        }

        ProductColor saved = colorRepository.save(color);
        cacheEvictor.evictProduct(productId);
        return productMapper.toProductColorResponse(saved);
    }

    @Override
    @Transactional
    public void deleteColor(Long productId, Long colorId) {
        ProductColor color = requireColor(productId, colorId);
        if (variantRepository.existsByColorId(colorId)) {
            throw new AppException(ErrorCode.PRODUCT_COLOR_HAS_VARIANTS);
        }

        List<ProductImage> images = imageRepository.findByColorIdOrderByOrderIndexAsc(colorId);
        List<String> publicIds = images.stream()
                .map(ProductImage::getProviderPublicId)
                .collect(Collectors.toList());

        imageRepository.deleteAll(images);
        colorRepository.delete(color);

        cacheEvictor.evictProduct(productId);
        publicIds.forEach(this::deleteCloudFileAfterCommit);
    }

    // =========================================================================
    // IMAGE
    // =========================================================================

    @Override
    @Transactional
    public ProductImageResponse addImage(Long productId, ProductImageRequest request) {
        ProductColor color = requireColor(productId, request.getColorId());

        ImagePriority priority = request.getIsMain();
        if (priority == null) {
            priority = imageRepository.hasImageWithPriority(color.getId(), ImagePriority.MAIN)
                    ? ImagePriority.SECONDARY
                    : ImagePriority.MAIN;
        }

        ProductImage image = ProductImage.builder()
                .color(color)
                .url(request.getUrl())
                .providerPublicId(request.getProviderPublicId() != null ? request.getProviderPublicId() : "")
                .title(request.getTitle())
                .altText(request.getAltText())
                .isMain(priority)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();

        ProductImage saved = imageRepository.save(image);

        // Mỗi màu chỉ có 1 ảnh MAIN.
        if (saved.getIsMain() == ImagePriority.MAIN) {
            imageRepository.demoteOthers(color.getId(), saved.getId(), ImagePriority.SECONDARY);
        }

        cacheEvictor.evictProduct(productId);
        return productMapper.toProductImageResponse(saved);
    }

    @Override
    @Transactional
    public ProductImageResponse updateImage(Long productId, Long imageId, ProductImageRequest request) {
        ProductImage image = requireImage(productId, imageId);

        Long oldColorId = image.getColor().getId();
        String oldPublicId = image.getProviderPublicId();

        // Cho phép chuyển ảnh sang màu khác, nhưng màu mới cũng phải thuộc cùng product.
        if (!oldColorId.equals(request.getColorId())) {
            image.setColor(requireColor(productId, request.getColorId()));
        }

        image.setUrl(request.getUrl());
        if (request.getProviderPublicId() != null) {
            image.setProviderPublicId(request.getProviderPublicId());
        }
        image.setTitle(request.getTitle());
        image.setAltText(request.getAltText());
        if (request.getIsMain() != null) {
            image.setIsMain(request.getIsMain());
        }
        if (request.getOrderIndex() != null) {
            image.setOrderIndex(request.getOrderIndex());
        }

        ProductImage saved = imageRepository.save(image);
        Long newColorId = saved.getColor().getId();

        if (saved.getIsMain() == ImagePriority.MAIN) {
            imageRepository.demoteOthers(newColorId, saved.getId(), ImagePriority.SECONDARY);
        }
        // Đảm bảo cả màu mới lẫn màu cũ (nếu có chuyển) vẫn còn đúng 1 ảnh MAIN.
        ensureMainImage(newColorId);
        if (!oldColorId.equals(newColorId)) {
            ensureMainImage(oldColorId);
        }

        // Thay file mới (publicId đổi) thì file cũ trên Cloudinary không còn dùng nữa.
        if (request.getProviderPublicId() != null && !request.getProviderPublicId().equals(oldPublicId)) {
            deleteCloudFileAfterCommit(oldPublicId);
        }

        cacheEvictor.evictProduct(productId);
        return productMapper.toProductImageResponse(saved);
    }

    @Override
    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = requireImage(productId, imageId);

        Long colorId = image.getColor().getId();
        String publicId = image.getProviderPublicId();

        imageRepository.delete(image);
        imageRepository.flush();

        // Xóa đúng ảnh MAIN thì đẩy ảnh kế tiếp lên làm MAIN.
        ensureMainImage(colorId);

        cacheEvictor.evictProduct(productId);
        deleteCloudFileAfterCommit(publicId);
    }

    // ------------------------------ helpers ------------------------------

    private ProductColor requireColor(Long productId, Long colorId) {
        return colorRepository.findByIdAndProductId(colorId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_COLOR_NOT_FOUND));
    }

    private ProductImage requireImage(Long productId, Long imageId) {
        return imageRepository.findByIdAndColorProductId(imageId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));
    }

    /** Màu nào có ảnh mà chưa có ảnh MAIN thì lấy ảnh đầu tiên (theo orderIndex) làm MAIN. */
    private void ensureMainImage(Long colorId) {
        if (imageRepository.hasImageWithPriority(colorId, ImagePriority.MAIN)) {
            return;
        }
        List<ProductImage> images = imageRepository.findByColorIdOrderByOrderIndexAsc(colorId);
        if (!images.isEmpty()) {
            ProductImage first = images.get(0);
            first.setIsMain(ImagePriority.MAIN);
            imageRepository.save(first);
        }
    }

    /** Xóa file Cloudinary sau khi DB commit; lỗi Cloudinary chỉ log, không làm hỏng thao tác chính. */
    private void deleteCloudFileAfterCommit(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        runAfterCommit(() -> {
            try {
                fileService.delete(publicId);
            } catch (Exception e) {
                log.warn("Could not delete Cloudinary file {}", publicId, e);
            }
        });
    }

    /**
     * Chạy SAU khi transaction DB commit thành công (không có transaction thì chạy ngay).
     * Nếu xóa file Cloudinary trước mà transaction rollback thì file mất oan.
     */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}