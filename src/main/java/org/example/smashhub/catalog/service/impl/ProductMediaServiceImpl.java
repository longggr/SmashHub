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
import org.example.smashhub.file.dto.FileUploadResponse;
import org.example.smashhub.file.enums.FileFolder;
import org.example.smashhub.file.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
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

        // Thu thập publicId của tất cả ảnh thuộc màu này TRƯỚC KHI xóa.
        List<ProductImage> images = imageRepository.findByColorIdOrderByOrderIndexAsc(colorId);
        List<String> publicIds = images.stream()
                .map(ProductImage::getProviderPublicId)
                .filter(pid -> pid != null && !pid.isBlank())
                .collect(Collectors.toList());

        imageRepository.deleteAll(images);
        colorRepository.delete(color);

        cacheEvictor.evictProduct(productId);
        // Xóa ảnh Cloudinary SAU KHI transaction commit thành công.
        publicIds.forEach(fileService::deleteAfterCommit);
    }

    // =========================================================================
    // IMAGE
    // =========================================================================

    @Override
    @Transactional
    public ProductImageResponse addImage(Long productId, ProductImageRequest request, MultipartFile file) {
        // Phải có file hoặc url hợp lệ. Kiểm tra sớm để request thiếu dữ liệu không đụng DB/Cloudinary.
        boolean hasFile = fileService.isPresent(file);
        if (!hasFile && isBlank(request.getUrl())) {
            throw new AppException(ErrorCode.PRODUCT_IMAGE_URL_INVALID);
        }

        ProductColor color = requireColor(productId, request.getColorId());

        String url = request.getUrl();
        String publicId = request.getProviderPublicId();

        if (hasFile) {
            // Upload vào đúng thư mục sản phẩm; rollback transaction → ảnh bị dọn tự động.
            FileUploadResponse uploaded = fileService.uploadInTransaction(file, FileFolder.PRODUCT);
            url = uploaded.getUrl();
            publicId = uploaded.getPublicId();
        }

        ImagePriority priority = request.getIsMain();
        if (priority == null) {
            // Mặc định: nếu màu chưa có ảnh MAIN thì ảnh mới là MAIN, ngược lại là SECONDARY.
            priority = imageRepository.hasImageWithPriority(color.getId(), ImagePriority.MAIN)
                    ? ImagePriority.SECONDARY
                    : ImagePriority.MAIN;
        }

        ProductImage image = ProductImage.builder()
                .color(color)
                .url(url)
                // Lưu publicId; nếu ảnh từ URL ngoài không có publicId thì để trống (không null để tránh NPE).
                .providerPublicId(publicId != null ? publicId : "")
                .title(request.getTitle())
                .altText(request.getAltText())
                .isMain(priority)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();

        ProductImage saved = imageRepository.save(image);

        // Mỗi màu chỉ có 1 ảnh MAIN — hạ cấp các ảnh còn lại.
        if (saved.getIsMain() == ImagePriority.MAIN) {
            imageRepository.demoteOthers(color.getId(), saved.getId(), ImagePriority.SECONDARY);
        }

        cacheEvictor.evictProduct(productId);
        return productMapper.toProductImageResponse(saved);
    }

    @Override
    @Transactional
    public ProductImageResponse updateImage(Long productId, Long imageId,
                                            ProductImageRequest request, MultipartFile file) {
        ProductImage image = requireImage(productId, imageId);

        Long oldColorId = image.getColor().getId();
        String oldPublicId = image.getProviderPublicId();

        // Cho phép chuyển ảnh sang màu khác trong cùng product.
        if (!oldColorId.equals(request.getColorId())) {
            image.setColor(requireColor(productId, request.getColorId()));
        }

        if (fileService.isPresent(file)) {
            // Có file mới: upload → gán url và publicId mới; rollback → ảnh mới bị dọn.
            FileUploadResponse uploaded = fileService.uploadInTransaction(file, FileFolder.PRODUCT);
            image.setUrl(uploaded.getUrl());
            image.setProviderPublicId(uploaded.getPublicId());
        } else {
            // Không có file: chỉ cập nhật url / publicId nếu client gửi tường minh.
            if (!isBlank(request.getUrl())) {
                image.setUrl(request.getUrl());
            }
            if (request.getProviderPublicId() != null) {
                image.setProviderPublicId(request.getProviderPublicId());
            }
        }

        if (request.getTitle() != null) {
            image.setTitle(request.getTitle());
        }
        if (request.getAltText() != null) {
            image.setAltText(request.getAltText());
        }
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
        // Đảm bảo cả màu mới lẫn màu cũ (nếu chuyển màu) đều còn đúng 1 ảnh MAIN.
        ensureMainImage(newColorId);
        if (!oldColorId.equals(newColorId)) {
            ensureMainImage(oldColorId);
        }

        // publicId thực sự đổi → ảnh cũ trên Cloudinary không còn dùng, xóa sau commit.
        // Không xóa nếu publicId trống (ảnh ngoài không có publicId Cloudinary).
        String newPublicId = saved.getProviderPublicId();
        if (!Objects.equals(oldPublicId, newPublicId)
                && oldPublicId != null && !oldPublicId.isBlank()) {
            fileService.deleteAfterCommit(oldPublicId);
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

        // Xóa đúng ảnh MAIN → đẩy ảnh kế tiếp lên làm MAIN.
        ensureMainImage(colorId);

        cacheEvictor.evictProduct(productId);

        // Xóa Cloudinary SAU COMMIT; bỏ qua nếu publicId trống (ảnh URL ngoài).
        if (publicId != null && !publicId.isBlank()) {
            fileService.deleteAfterCommit(publicId);
        }
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

    /** Màu nào có ảnh mà chưa có ảnh MAIN → lấy ảnh đầu tiên (theo orderIndex) làm MAIN. */
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}