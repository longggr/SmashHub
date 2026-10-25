package org.example.smashhub.catalog.service;

import org.example.smashhub.catalog.dto.request.ProductColorRequest;
import org.example.smashhub.catalog.dto.request.ProductImageRequest;
import org.example.smashhub.catalog.dto.response.ProductColorResponse;
import org.example.smashhub.catalog.dto.response.ProductImageResponse;

public interface ProductMediaService {
    ProductColorResponse addColor(Long productId, ProductColorRequest request);
    ProductColorResponse updateColor(Long productId, Long colorId, ProductColorRequest request);
    /** Chỉ xóa được khi màu không còn variant; ảnh của màu bị xóa theo (cả file trên Cloudinary, sau commit). */
    void deleteColor(Long productId, Long colorId);

    // ----- Image (colorId nằm trong request) -----
    ProductImageResponse addImage(Long productId, ProductImageRequest request);
    ProductImageResponse updateImage(Long productId, Long imageId, ProductImageRequest request);
    void deleteImage(Long productId, Long imageId);
}
