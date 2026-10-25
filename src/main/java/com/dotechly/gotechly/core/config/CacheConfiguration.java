package com.dotechly.gotechly.core.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache configuration for reference data lookups.
 *
 * Caches static/slow-changing data that is queried on every payment request:
 * channels, error codes, app configs, endpoint configs.
 *
 * Uses ConcurrentMapCacheManager (in-memory, no TTL) since this data
 * rarely changes and cache eviction happens explicitly on save/delete.
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    public static final String CACHE_CHANNELS = "channels";
    public static final String CACHE_ERROR_CODES = "errorCodes";
    public static final String CACHE_ERROR_CODES_BY_API = "errorCodesByApi";
    public static final String CACHE_ERROR_CODE_DESCRIPTIONS = "errorCodeDescriptions";
    public static final String CACHE_APP_CONFIGS = "appConfigs";
    public static final String CACHE_APP_CONFIGS_BY_PREFIX = "appConfigsByPrefix";
    public static final String CACHE_ENDPOINT_CONFIGS = "endpointConfigs";
    public static final String CACHE_PARTNERS = "partners";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CACHE_CHANNELS,
                CACHE_ERROR_CODES,
                CACHE_ERROR_CODES_BY_API,
                CACHE_ERROR_CODE_DESCRIPTIONS,
                CACHE_APP_CONFIGS,
                CACHE_APP_CONFIGS_BY_PREFIX,
                CACHE_ENDPOINT_CONFIGS,
                CACHE_PARTNERS
        );
    }
}


