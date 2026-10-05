package org.example.smashhub.catalog.specification;

import jakarta.persistence.criteria.*;
import org.example.smashhub.catalog.dto.request.ProductAttributeFilter;
import org.example.smashhub.catalog.entity.Product;
import org.example.smashhub.catalog.entity.ProductAttribute;
import org.example.smashhub.common.enums.ProductStatus;
import org.example.smashhub.common.enums.ProductType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public class ProductSpecification {
    private ProductSpecification(){}

    public static Specification<Product> hasKeyword(String keyword){
        if(keyword == null || keyword.isBlank()) return null;
        String pattern = "%" + keyword.trim().toLowerCase()+"%";

        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }
    public static Specification<Product> hasCategory(Long categoryId){
        if(categoryId == null) return null;
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }
    public static Specification<Product> hasBrand(Long brandId){
        if(brandId == null) return  null;
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("brand").get("id"), brandId);
    }
    public static Specification<Product> hasType(ProductType type) {
        if (type == null) return null;
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Product> hasStatus(ProductStatus status){
        if(status == null) return null;
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("productStatus"), status);
    }
    private static Expression<BigDecimal> effectivePrice(Root<Product> root, CriteriaBuilder cb) {
        return cb.coalesce(root.get("salePrice"), root.get("price"));
    }
    public static Specification<Product> priceFrom(BigDecimal minPrice){
        if (minPrice == null) return null;
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(effectivePrice(root,criteriaBuilder), minPrice);
    }
    public static Specification<Product> priceTo(BigDecimal maxPrice){
        if (maxPrice == null) return null;
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(effectivePrice(root,criteriaBuilder),maxPrice);
    }

    public static Specification<Product> onSaleOnly(Boolean onSale){
        if (onSale == null || !onSale) return null;
        return (root, query, criteriaBuilder) -> {
           Predicate notNull = criteriaBuilder.isNotNull(root.get("salePrice"));
            Predicate lowerThanPrice = criteriaBuilder.lessThan(root.get("salePrice"), root.get("price"));
            return criteriaBuilder.and(notNull, lowerThanPrice);
        };
    }

    public static Specification<Product> hasAttribute(String attrName,String attrValue){
        if(attrName ==null||attrName.isBlank()||attrValue==null||attrValue.isBlank())
            return null;
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var attrRoot = subquery.from(ProductAttribute.class);
            subquery.select(attrRoot.get("product").get("id"));
            subquery.where(
                    cb.equal(cb.lower(attrRoot.get("attrName")), attrName.trim().toLowerCase()),
                    cb.equal(cb.lower(attrRoot.get("attrValue")), attrValue.trim().toLowerCase())
            );
            return cb.in(root.get("id")).value(subquery);
        };
    }

    public static Specification<Product> hasAttributes(List<ProductAttributeFilter> attributes) {
        if (attributes == null || attributes.isEmpty()) return null;

        Specification<Product> combined = Specification.allOf();
        for (ProductAttributeFilter filter : attributes) {
            Specification<Product> one = hasAttribute(filter.getAttrName(), filter.getAttrValue());
            if (one != null) combined = combined.and(one); // Nối đệ quy các Specification
        }
        return combined;
    }

    public static Specification<Product> build(String keyword,
                                               Long categoryId,
                                               Long brandId,
                                               ProductType type,
                                               ProductStatus status,
                                               BigDecimal minPrice,
                                               BigDecimal maxPrice,
                                               Boolean onSale,
                                               List<ProductAttributeFilter> attributes) {
        // allOf() sẽ tự động bỏ qua (ignore) bất kỳ Specification nào trả về null
        return Specification.allOf(
                hasKeyword(keyword),
                hasCategory(categoryId),
                hasBrand(brandId),
                hasType(type),
                hasStatus(status),
                priceFrom(minPrice),
                priceTo(maxPrice),
                onSaleOnly(onSale),
                hasAttributes(attributes)
        );
    }
}
