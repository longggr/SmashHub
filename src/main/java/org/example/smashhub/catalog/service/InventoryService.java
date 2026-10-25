package org.example.smashhub.catalog.service;

import org.example.smashhub.catalog.dto.request.ProductVariantRequest;
import org.example.smashhub.catalog.dto.response.ProductVariantResponse;

public interface InventoryService {

    ProductVariantResponse addVariant(Long productId, ProductVariantRequest request);

    ProductVariantResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request);

    void deleteVariant(Long productId, Long variantId);

    ProductVariantResponse deductStock(Long variantId, int quantity);

    ProductVariantResponse restoreStock(Long variantId, int quantity);
}
