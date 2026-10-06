package org.example.smashhub.catalog.cache;

import lombok.AccessLevel;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogCacheEvictor {
    CacheManager cacheManager;

    public void evictProduct(Long productId){
        Cache productCache = cacheManager.getCache("product");
        if(productCache != null && productId != null){
            productCache.evict(productId);
        }

        Cache productBySlugCache = cacheManager.getCache("productBySlug");
        if(productBySlugCache !=null){
            productBySlugCache.clear();
        }
    }
}
