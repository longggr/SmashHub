package org.example.smashhub.catalog.service;

import org.example.smashhub.catalog.dto.request.ProductColorRequest;
import org.example.smashhub.catalog.dto.request.ProductImageRequest;
import org.example.smashhub.catalog.dto.response.ProductColorResponse;
import org.example.smashhub.catalog.dto.response.ProductImageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProductMediaService {
    ProductColorResponse addColor(Long productId, ProductColorRequest request);
    ProductColorResponse updateColor(Long productId, Long colorId, ProductColorRequest request);
    void deleteColor(Long productId, Long colorId);
    // ----- Image (colorId nằm trong request) -----
    ProductImageResponse addImage(Long productId, ProductImageRequest request, MultipartFile file);
    ProductImageResponse updateImage(Long productId, Long imageId, ProductImageRequest request, MultipartFile file);
    void deleteImage(Long productId, Long imageId);
}
