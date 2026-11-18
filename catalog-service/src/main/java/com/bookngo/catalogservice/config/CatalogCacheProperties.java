package com.bookngo.catalogservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "app.cache")
public record CatalogCacheProperties(Map<String, Spec> specs) {

    public CatalogCacheProperties {
        specs = specs == null ? Map.of() : Map.copyOf(specs);
    }

    public record Spec(Duration ttl, long maxSize) {
    }
}
