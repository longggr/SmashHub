package org.example.smashhub.catalog.service;

import org.example.smashhub.catalog.dto.request.CategoryRequest;
import org.example.smashhub.catalog.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Long id, CategoryRequest request);
    void delete(Long id);
    CategoryResponse getById(Long id);
    CategoryResponse getBySlug(String slug);
    List<CategoryResponse> getTree();

    List<CategoryResponse> getChildrenByParentId(Long parentId);
    List<Long> getSelfAndDescendantIds(Long categoryId);
}
