package org.example.smashhub.catalog.cache;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogCacheEvictor {
    CacheManager cacheManager;

    /** Xóa cache của 1 sản phẩm (theo id) và toàn bộ cache theo slug. Chạy SAU commit. */
    public void evictProduct(Long productId) {
        runAfterCommit(() -> {
            evictKey("product", productId);
            clear("productBySlug");
        });
    }

    /**
     * Danh mục thay đổi (đổi tên/slug/cha) -> tên danh mục nhúng trong ProductResponse bị cũ,
     * nên phải xóa luôn cache sản phẩm.
     */
    public void evictCategories() {
        runAfterCommit(() -> {
            clear("categoryTree");
            clear("category");
            clear("product");
            clear("productBySlug");
        });
    }

    /**
     * Cache brand chứa productCount nên cũng phải xóa mỗi khi sản phẩm được tạo/sửa/xóa.
     */
    public void evictBrands() {
        runAfterCommit(() -> {
            clear("brands");
            clear("brand");
        });
    }

    // Lỗi Redis không được làm hỏng request ghi DB đã commit -> chỉ log cảnh báo (cache sẽ tự hết hạn theo TTL).
    private void evictKey(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null && key != null) {
                cache.evict(key);
            }
        } catch (Exception e) {
            log.warn("Could not evict key {} from cache {}", key, cacheName, e);
        }
    }

    private void clear(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            log.warn("Could not clear cache {}", cacheName, e);
        }
    }

    /**
     * Chạy SAU khi transaction DB commit thành công (không có transaction thì chạy ngay).
     * Xóa cache ngay trong transaction thì request khác có thể đọc DB cũ rồi nạp lại cache cũ.
     */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}