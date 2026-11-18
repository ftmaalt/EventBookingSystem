package com.bookngo.catalogservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Duration;
import java.util.List;

// Cache advice wraps the transaction advice, so evictions run only after the write has committed.
@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
@EnableConfigurationProperties(CatalogCacheProperties.class)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(CatalogCacheProperties properties) {
        List<CaffeineCache> caches = CacheNames.ALL.stream()
                .map(name -> buildCache(name, properties.specs().get(name)))
                .toList();
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, CatalogCacheProperties.Spec spec) {
        if (spec == null || spec.ttl() == null || spec.ttl().compareTo(Duration.ZERO) <= 0 || spec.maxSize() <= 0) {
            throw new IllegalStateException("Missing or invalid cache settings for '" + name
                    + "': app.cache.specs." + name + ".ttl and .max-size must be positive.");
        }
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(spec.ttl())
                .maximumSize(spec.maxSize())
                .recordStats()
                .build(), false);
    }
}
