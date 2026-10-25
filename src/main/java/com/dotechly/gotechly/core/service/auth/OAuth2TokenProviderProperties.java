package com.dotechly.gotechly.core.service.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Bound to {@code gotechly.oauth2-token-provider.*} in {@code application.yml}.
 * Sized for sparse traffic — one POST per registered profile per refresh interval.
 */
@ConfigurationProperties("gotechly.oauth2-token-provider")
public record OAuth2TokenProviderProperties(
        Duration connectTimeout,
        Duration responseTimeout,
        Duration readTimeout,
        Duration writeTimeout,
        Pool pool,
        Scheduler refreshScheduler,
        Backoff backoff,
        boolean trustAllCerts) {

    public record Pool(
            int maxConnections,
            int pendingAcquireMaxCount,
            Duration pendingAcquireTimeout,
            Duration maxIdleTime,
            Duration maxLifeTime,
            Duration evictInBackground) {}

    public record Scheduler(int corePoolSize) {}

    public record Backoff(
            Duration retryableDelay,
            Duration nonRetryableDelay) {}
}
