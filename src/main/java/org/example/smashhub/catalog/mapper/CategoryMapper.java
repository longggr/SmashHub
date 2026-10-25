package org.example.smashhub.catalog.mapper;

import org.example.smashhub.catalog.dto.request.CategoryRequest;
import org.example.smashhub.catalog.dto.response.CategoryResponse;
import org.example.smashhub.catalog.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {
    @Mapping(target = "parent", ignore = true)
    Category toCategory(CategoryRequest request);

    @Mapping(target = "parent", ignore = true)
    void updateCategory(@MappingTarget Category category, CategoryRequest request);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toCategoryResponse(Category category);

    List<CategoryResponse> toCategoryResponseList(List<Category> categories);
}
