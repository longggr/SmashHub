package org.example.smashhub.catalog.service;

import org.example.smashhub.catalog.dto.request.ProductAttributeRequest;
import org.example.smashhub.catalog.dto.response.ProductAttributeResponse;

public interface ProductAttributeService {
    ProductAttributeResponse addAttribute(Long productId, ProductAttributeRequest request);

    ProductAttributeResponse updateAttribute(Long productId, Long attributeId, ProductAttributeRequest request);

    void deleteAttribute(Long productId, Long attributeId);
}
