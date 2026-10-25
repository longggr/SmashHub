package org.example.smashhub.catalog.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.catalog.cache.CatalogCacheEvictor;
import org.example.smashhub.catalog.dto.request.ProductAttributeRequest;
import org.example.smashhub.catalog.dto.response.ProductAttributeResponse;
import org.example.smashhub.catalog.entity.Product;
import org.example.smashhub.catalog.entity.ProductAttribute;
import org.example.smashhub.catalog.mapper.ProductMapper;
import org.example.smashhub.catalog.repository.ProductAttributeRepository;
import org.example.smashhub.catalog.repository.ProductRepository;
import org.example.smashhub.catalog.service.ProductAttributeService;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductAttributeServiceImpl implements ProductAttributeService {

    ProductRepository productRepository;
    ProductAttributeRepository attributeRepository;
    ProductMapper productMapper;
    CatalogCacheEvictor cacheEvictor;

    @Override
    @Transactional
    public ProductAttributeResponse addAttribute(Long productId, ProductAttributeRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductAttribute attribute = ProductAttribute.builder()
                .product(product)
                .attrName(request.getAttrName().trim())
                .attrValue(request.getAttrValue().trim())
                .build();

        ProductAttribute saved = attributeRepository.save(attribute);
        cacheEvictor.evictProduct(productId);
        return productMapper.toProductAttributeResponse(saved);
    }

    @Override
    @Transactional
    public ProductAttributeResponse updateAttribute(Long productId, Long attributeId, ProductAttributeRequest request) {
        ProductAttribute attribute = attributeRepository.findByIdAndProductId(attributeId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_ATTRIBUTE_NOT_FOUND));

        attribute.setAttrName(request.getAttrName().trim());
        attribute.setAttrValue(request.getAttrValue().trim());

        ProductAttribute saved = attributeRepository.save(attribute);
        cacheEvictor.evictProduct(productId);
        return productMapper.toProductAttributeResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAttribute(Long productId, Long attributeId) {
        ProductAttribute attribute = attributeRepository.findByIdAndProductId(attributeId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_ATTRIBUTE_NOT_FOUND));

        attributeRepository.delete(attribute);
        cacheEvictor.evictProduct(productId);
    }
}