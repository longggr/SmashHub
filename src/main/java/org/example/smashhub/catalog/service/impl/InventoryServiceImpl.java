package org.example.smashhub.catalog.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.catalog.cache.CatalogCacheEvictor;
import org.example.smashhub.catalog.dto.request.ProductVariantRequest;
import org.example.smashhub.catalog.dto.response.ProductVariantResponse;
import org.example.smashhub.catalog.entity.ProductColor;
import org.example.smashhub.catalog.entity.ProductVariant;
import org.example.smashhub.catalog.mapper.ProductMapper;
import org.example.smashhub.catalog.repository.ProductColorRepository;
import org.example.smashhub.catalog.repository.ProductVariantRepository;
import org.example.smashhub.catalog.service.InventoryService;
import org.example.smashhub.common.enums.InventoryStatus;
import org.example.smashhub.common.enums.VariantStatus;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryServiceImpl implements InventoryService {

    ProductColorRepository colorRepository;
    ProductVariantRepository variantRepository;
    ProductMapper productMapper;
    CatalogCacheEvictor cacheEvictor;

    // =========================================================================
    // QUẢN TRỊ VARIANT
    // =========================================================================

    @Override
    @Transactional
    public ProductVariantResponse addVariant(Long productId, ProductVariantRequest request) {
        String sku = request.getSku().trim();
        if (variantRepository.existsBySku(sku)) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_SKU_EXISTED);
        }

        ProductColor color = requireColor(productId, request.getColorId());

        ProductVariant variant = ProductVariant.builder()
                .color(color)
                .sku(sku)
                .size(request.getSize())
                .stock(request.getStock())
                .active(request.getActive() != null ? request.getActive() : VariantStatus.ACTIVE)
                .inventoryStatus(resolveInventoryStatus(request.getStock(), request.getInventoryStatus()))
                .build();

        ProductVariant saved = saveVariant(variant);
        cacheEvictor.evictProduct(productId);
        return productMapper.toProductVariantResponse(saved);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request) {
        // Khóa dòng (FOR UPDATE): deductStock đang chạy song song sẽ phải chờ, không bị ghi đè stock.
        ProductVariant variant = variantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        if (!variant.getColor().getProduct().getId().equals(productId)) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }

        String sku = request.getSku().trim();
        if (!variant.getSku().equalsIgnoreCase(sku) && variantRepository.existsBySku(sku)) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_SKU_EXISTED);
        }

        // Cho phép chuyển variant sang màu khác của cùng product.
        if (!variant.getColor().getId().equals(request.getColorId())) {
            variant.setColor(requireColor(productId, request.getColorId()));
        }

        variant.setSku(sku);
        variant.setSize(request.getSize());
        variant.setStock(request.getStock());
        if (request.getActive() != null) {
            variant.setActive(request.getActive());
        }
        variant.setInventoryStatus(resolveInventoryStatus(request.getStock(), request.getInventoryStatus()));

        ProductVariant saved = saveVariant(variant);
        cacheEvictor.evictProduct(productId);
        return productMapper.toProductVariantResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant variant = variantRepository.findByIdAndColorProductId(variantId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        // Xóa mềm: giữ lại dòng để đơn hàng cũ / giỏ hàng vẫn tham chiếu được (không vi phạm khóa ngoại).
        variant.setActive(VariantStatus.INACTIVE);
        variantRepository.save(variant);
        cacheEvictor.evictProduct(productId);
    }

    // =========================================================================
    // TỒN KHO (gọi nội bộ từ order/checkout)
    // =========================================================================

    /**
     * Trừ tồn kho khi checkout. An toàn khi nhiều request đồng thời nhờ câu UPDATE nguyên tử
     * (điều kiện stock >= qty nằm ngay trong câu lệnh), không cần Redis lock.
     */
    @Override
    @Transactional
    public ProductVariantResponse deductStock(Long variantId, int quantity) {
        if (quantity <= 0) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_QUANTITY_INVALID);
        }

        int updated = variantRepository.deductStockAtomic(variantId, quantity, VariantStatus.ACTIVE);
        if (updated == 0) {
            // 0 dòng: phân biệt 3 nguyên nhân để trả đúng lỗi cho client.
            ProductVariant variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
            if (variant.getActive() != VariantStatus.ACTIVE) {
                throw new AppException(ErrorCode.PRODUCT_VARIANT_INACTIVE);
            }
            throw new AppException(ErrorCode.PRODUCT_VARIANT_INSUFFICIENT_STOCK,
                    "Insufficient stock for SKU: " + variant.getSku());
        }

        // Hết hàng thì đổi trạng thái (câu lệnh riêng, chạy khi dòng còn đang được khóa bởi transaction này).
        variantRepository.markOutOfStockIfEmpty(variantId, InventoryStatus.OUT_OF_STOCK);

        ProductVariant fresh = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        cacheEvictor.evictProduct(fresh.getColor().getProduct().getId());
        return productMapper.toProductVariantResponse(fresh);
    }

    /**
     * Hoàn kho khi hủy đơn / hết hạn thanh toán.
     * LƯU Ý: hàm này chưa idempotent. OrderService phải đảm bảo mỗi đơn chỉ hoàn kho đúng một lần
     * (vd kiểm tra chuyển trạng thái PENDING_PAYMENT -> CANCELLED/EXPIRED trong cùng transaction).
     */
    @Override
    @Transactional
    public ProductVariantResponse restoreStock(Long variantId, int quantity) {
        if (quantity <= 0) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_QUANTITY_INVALID);
        }

        if (variantRepository.restoreStockAtomic(variantId, quantity) == 0) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }
        variantRepository.markInStockIfAvailable(variantId, InventoryStatus.IN_STOCK);

        ProductVariant fresh = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        cacheEvictor.evictProduct(fresh.getColor().getProduct().getId());
        return productMapper.toProductVariantResponse(fresh);
    }

    // ------------------------------ helpers ------------------------------

    private ProductColor requireColor(Long productId, Long colorId) {
        return colorRepository.findByIdAndProductId(colorId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_COLOR_NOT_FOUND));
    }

    /**
     * Có hàng -> luôn IN_STOCK. Hết hàng -> OUT_OF_STOCK, trừ khi admin chủ động đặt BACKORDER/IN_ORDER.
     * (Không để admin set IN_STOCK khi stock = 0 như trước.)
     */
    private InventoryStatus resolveInventoryStatus(int stock, InventoryStatus requested) {
        if (stock > 0) {
            return InventoryStatus.IN_STOCK;
        }
        if (requested == InventoryStatus.BACKORDER || requested == InventoryStatus.IN_ORDER) {
            return requested;
        }
        return InventoryStatus.OUT_OF_STOCK;
    }

    /** Race giữa 2 request cùng tạo 1 SKU: unique constraint của DB chặn lần ghi thứ hai. */
    private ProductVariant saveVariant(ProductVariant variant) {
        try {
            return variantRepository.saveAndFlush(variant);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_SKU_EXISTED);
        }
    }
}