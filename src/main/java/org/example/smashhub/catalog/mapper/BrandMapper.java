package org.example.smashhub.catalog.mapper;

import org.example.smashhub.catalog.dto.request.BrandRequest;
import org.example.smashhub.catalog.dto.response.BrandResponse;
import org.example.smashhub.catalog.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BrandMapper {
    Brand toBrand(BrandRequest request);
    BrandResponse toBrandResponse(Brand brand);
    List<BrandResponse> toBrandResponseList(List<Brand> brands);
    void updateBrand(@MappingTarget Brand brand, BrandRequest request);
}
