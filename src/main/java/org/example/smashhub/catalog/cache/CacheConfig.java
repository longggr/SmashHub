package org.example.smashhub.catalog.cache;

import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.catalog.dto.response.BrandResponse;
import org.example.smashhub.catalog.dto.response.CategoryResponse;
import org.example.smashhub.catalog.dto.response.ProductResponse;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cấu hình cache Redis cho module catalog.
 *
 * <p>Mỗi cache chỉ chứa đúng một kiểu giá trị nên mỗi cache có serializer riêng theo kiểu đó
 * ({@link TypedJsonRedisSerializer}), không dùng default typing: JSON không chứa tên class,
 * nên không phụ thuộc cấu hình serializer cũ hay class nội bộ của JDK (List.of, Stream.toList...).
 *
 * <p><b>Khi đổi cấu trúc DTO được cache (đổi tên/kiểu field, không phải thêm field):</b> tăng {@link #KEY_PREFIX}
 * (v1 -> v2). Entry cũ bị bỏ qua và tự hết hạn theo TTL, không cần flush Redis bằng tay.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /** Tiền tố version của mọi key cache catalog: "catalog:v1::brands::all". */
    static final String KEY_PREFIX = "catalog:v1::";

    // Phải trùng với cacheNames trong @Cacheable / CatalogCacheEvictor.
    private static final String CACHE_BRANDS = "brands";
    private static final String CACHE_BRAND = "brand";
    private static final String CACHE_CATEGORY_TREE = "categoryTree";
    private static final String CACHE_CATEGORY = "category";
    private static final String CACHE_PRODUCT = "product";
    private static final String CACHE_PRODUCT_BY_SLUG = "productBySlug";

    private static final Duration TTL_REFERENCE_DATA = Duration.ofMinutes(30);
    // Product chứa tồn kho nên TTL ngắn hơn: dù sót evict thì dữ liệu cũ cũng chỉ tồn tại vài phút.
    private static final Duration TTL_PRODUCT = Duration.ofMinutes(5);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(KEY_PREFIX)
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CACHE_BRAND, typed(base, TTL_REFERENCE_DATA, TypedJsonRedisSerializer.of(BrandResponse.class)));
        perCache.put(CACHE_BRANDS, typed(base, TTL_REFERENCE_DATA, TypedJsonRedisSerializer.listOf(BrandResponse.class)));
        perCache.put(CACHE_CATEGORY, typed(base, TTL_REFERENCE_DATA, TypedJsonRedisSerializer.of(CategoryResponse.class)));
        perCache.put(CACHE_CATEGORY_TREE, typed(base, TTL_REFERENCE_DATA, TypedJsonRedisSerializer.listOf(CategoryResponse.class)));
        perCache.put(CACHE_PRODUCT, typed(base, TTL_PRODUCT, TypedJsonRedisSerializer.of(ProductResponse.class)));
        perCache.put(CACHE_PRODUCT_BY_SLUG, typed(base, TTL_PRODUCT, TypedJsonRedisSerializer.of(ProductResponse.class)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CatalogCacheErrorHandler();
    }

    private static RedisCacheConfiguration typed(RedisCacheConfiguration base, Duration ttl, RedisSerializer<?> valueSerializer) {
        return base.entryTtl(ttl).serializeValuesWith(SerializationPair.fromSerializer(valueSerializer));
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Serializer JSON có kiểu cố định cho một cache. Không ghi type id vào JSON; kiểu đọc ra do code quyết định
     * (List luôn là ArrayList); bỏ qua field lạ nên thêm field vào DTO không làm hỏng entry cũ.
     */
    static final class TypedJsonRedisSerializer<T> implements RedisSerializer<T> {

        // JsonMapper của Jackson 3 là immutable và thread-safe; java.time đã có sẵn trong databind.
        private static final JsonMapper MAPPER = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        private static final byte[] EMPTY = new byte[0];

        private final JavaType type;

        private TypedJsonRedisSerializer(JavaType type) {
            this.type = type;
        }

        /** Cache chứa một object, ví dụ BrandResponse. */
        static <T> TypedJsonRedisSerializer<T> of(Class<T> valueType) {
            return new TypedJsonRedisSerializer<>(MAPPER.constructType(valueType));
        }

        /** Cache chứa List<E>, ví dụ List<BrandResponse>. Đọc ra luôn là ArrayList. */
        static <E> TypedJsonRedisSerializer<List<E>> listOf(Class<E> elementType) {
            JavaType listType = MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, elementType);
            return new TypedJsonRedisSerializer<>(listType);
        }

        @Override
        public byte[] serialize(T value) throws SerializationException {
            if (value == null) {
                return EMPTY;
            }
            try {
                return MAPPER.writeValueAsBytes(value);
            } catch (JacksonException e) {
                throw new SerializationException("Could not write JSON for cache value: " + e.getOriginalMessage(), e);
            }
        }

        @Override
        public T deserialize(byte[] bytes) throws SerializationException {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            try {
                return MAPPER.readValue(bytes, type);
            } catch (JacksonException e) {
                throw new SerializationException("Could not read JSON for cache value as " + type + ": "
                        + e.getOriginalMessage(), e);
            }
        }
    }

    /**
     * Cache chỉ là lớp tăng tốc, lỗi cache không được làm hỏng request.
     * Đọc lỗi (entry hỏng / sai định dạng / Redis chập chờn): cảnh báo, xoá entry đó, coi như cache miss để service
     * đọc DB rồi ghi lại entry đúng định dạng. Ghi / xoá / clear lỗi: chỉ cảnh báo, dữ liệu cũ tự hết hạn theo TTL.
     */
    @Slf4j
    static final class CatalogCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache GET failed (cache='{}', key='{}'), treating as miss: {}",
                    cache.getName(), key, exception.toString());
            try {
                cache.evict(key);
            } catch (RuntimeException evictFailure) {
                log.warn("Could not evict broken entry (cache='{}', key='{}'): {}",
                        cache.getName(), key, evictFailure.toString());
            }
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("Cache PUT failed (cache='{}', key='{}'): {}", cache.getName(), key, exception.toString());
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache EVICT failed (cache='{}', key='{}'): {}", cache.getName(), key, exception.toString());
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.warn("Cache CLEAR failed (cache='{}'): {}", cache.getName(), exception.toString());
        }
    }
}