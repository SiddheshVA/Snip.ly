package com.urlshortener.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache configuration.
 *
 * Cache name: "shortUrls"
 *   - Stores: shortCode → originalUrl
 *   - Max 10,000 entries
 *   - Expires 10 minutes after last write
 *
 * Why cache? Every redirect hits GET /{shortCode}.
 * With cache, DB is only hit on the FIRST request per code.
 * Subsequent requests are served from RAM in ~1ms.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("shortUrls");
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()    // Enable hit/miss metrics
        );
        return cacheManager;
    }
}
