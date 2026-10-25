package org.example.smashhub.catalog.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.catalog.cache.CatalogCacheEvictor;
import org.example.smashhub.catalog.dto.request.CategoryRequest;
import org.example.smashhub.catalog.dto.response.CategoryResponse;
import org.example.smashhub.catalog.entity.Category;
import org.example.smashhub.catalog.mapper.CategoryMapper;
import org.example.smashhub.catalog.repository.CategoryRepository;
import org.example.smashhub.catalog.repository.ProductRepository;
import org.example.smashhub.catalog.service.CategoryService;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {

    CategoryRepository categoryRepository;
    ProductRepository productRepository;
    CategoryMapper categoryMapper;
    CatalogCacheEvictor cacheEvictor;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug().trim())) {
            throw new AppException(ErrorCode.CATEGORY_SLUG_EXISTED);
        }

        Category category = categoryMapper.toCategory(request);
        category.setName(request.getName().trim());
        category.setSlug(request.getSlug().trim().toLowerCase());
        if (category.getDisplayOrder() == null) {
            category.setDisplayOrder(0);
        }

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_PARENT_NOT_FOUND));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        cacheEvictor.evictCategories();
        return categoryMapper.toCategoryResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!category.getSlug().equalsIgnoreCase(request.getSlug().trim())
                && categoryRepository.existsBySlug(request.getSlug().trim())) {
            throw new AppException(ErrorCode.CATEGORY_SLUG_EXISTED);
        }

        categoryMapper.updateCategory(category, request);
        category.setName(request.getName().trim());
        category.setSlug(request.getSlug().trim().toLowerCase());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id) || getSelfAndDescendantIds(id).contains(request.getParentId())) {
                throw new AppException(ErrorCode.CATEGORY_SELF_PARENT);
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_PARENT_NOT_FOUND));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category saved = categoryRepository.save(category);
        cacheEvictor.evictCategories();
        return categoryMapper.toCategoryResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!categoryRepository.findByParentId(id).isEmpty()) {
            throw new AppException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new AppException(ErrorCode.PRODUCT_CATEGORY_INVALID,
                    "Cannot delete category because it still contains products");
        }

        categoryRepository.delete(category);
        cacheEvictor.evictCategories();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "category", key = "'id:' + #id")
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        CategoryResponse response = categoryMapper.toCategoryResponse(category);
        response.setChildren(getChildrenByParentId(id));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "category", key = "'slug:' + #slug")
    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        CategoryResponse response = categoryMapper.toCategoryResponse(category);
        response.setChildren(getChildrenByParentId(category.getId()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildrenByParentId(Long parentId) {
        return categoryRepository.findByParentId(parentId).stream()
                .sorted(Comparator.comparing(Category::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoryTree", key = "'all'")
    public List<CategoryResponse> getTree() {
        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, CategoryResponse> dtoMap = allCategories.stream()
                .map(cat -> {
                    CategoryResponse res = categoryMapper.toCategoryResponse(cat);
                    res.setChildren(new ArrayList<>());
                    return res;
                })
                .collect(Collectors.toMap(CategoryResponse::getId, res -> res));

        List<CategoryResponse> roots = new ArrayList<>();

        for (CategoryResponse dto : dtoMap.values()) {
            if (dto.getParentId() == null) {
                roots.add(dto);
            } else {
                CategoryResponse parentDto = dtoMap.get(dto.getParentId());
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                }
            }
        }

        Comparator<CategoryResponse> orderComparator = Comparator.comparing(
                CategoryResponse::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())
        );
        roots.sort(orderComparator);
        dtoMap.values().forEach(node -> node.getChildren().sort(orderComparator));

        return roots;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getSelfAndDescendantIds(Long categoryId) {
        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, List<Long>> parentToChildrenMap = new HashMap<>();

        for (Category cat : allCategories) {
            if (cat.getParent() != null) {
                parentToChildrenMap
                        .computeIfAbsent(cat.getParent().getId(), k -> new ArrayList<>())
                        .add(cat.getId());
            }
        }

        List<Long> resultIds = new ArrayList<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            resultIds.add(currentId);
            queue.addAll(parentToChildrenMap.getOrDefault(currentId, Collections.emptyList()));
        }

        return resultIds;
    }
}