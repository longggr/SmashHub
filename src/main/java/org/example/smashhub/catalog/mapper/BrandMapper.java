package org.example.smashhub.catalog.mapper;

import org.example.smashhub.catalog.dto.request.BrandRequest;
import org.example.smashhub.catalog.dto.response.BrandResponse;
import org.example.smashhub.catalog.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BrandMapper {

    /**
     * Map request → entity khi tạo mới.
     * logoUrl và logoPublicId được bỏ qua vì chỉ được gán sau khi upload file thực tế.
     */
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "logoPublicId", ignore = true)
    Brand toBrand(BrandRequest request);

    BrandResponse toBrandResponse(Brand brand);

    List<BrandResponse> toBrandResponseList(List<Brand> brands);

    /**
     * Map request → entity khi cập nhật.
     * logoUrl và logoPublicId được bỏ qua để service tự kiểm soát:
     * - Không có file mới → giữ nguyên logo cũ.
     * - Có file mới → service gán sau khi upload.
     */
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "logoPublicId", ignore = true)
    void updateBrand(@MappingTarget Brand brand, BrandRequest request);
}
