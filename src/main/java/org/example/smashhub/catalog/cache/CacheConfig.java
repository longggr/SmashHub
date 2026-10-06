package org.example.smashhub.catalog.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableCaching
public class CacheConfig {

    private static final String CACHE_BRANDS = "brands";
    private static final String CACHE_BRAND = "brand";
    private static final String CACHE_CATEGORY_TREE = "categoryTree";
    private static final String CACHE_CATEGORY = "category";
    private static final String CACHE_PRODUCT = "product";
    private static final String CACHE_PRODUCT_BY_SLUG = "productBySlug";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build())
                .customize(mapperBuilder -> mapperBuilder.addModule(new JavaTimeModule()))
                .build();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> perCacheConfig = new HashMap<>();
        perCacheConfig.put(CACHE_BRANDS, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        perCacheConfig.put(CACHE_BRAND, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        perCacheConfig.put(CACHE_CATEGORY_TREE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        perCacheConfig.put(CACHE_CATEGORY, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        perCacheConfig.put(CACHE_PRODUCT, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        perCacheConfig.put(CACHE_PRODUCT_BY_SLUG, defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }
}