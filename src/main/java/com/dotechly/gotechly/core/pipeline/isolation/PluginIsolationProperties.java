package com.dotechly.gotechly.core.pipeline.isolation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Sizing for the per-payment-type bulkheads. See {@link PluginSchedulers} for why they exist.
 *
 * <pre>
 * gotechly:
 *   plugins:
 *     isolation:
 *       enabled: true
 *       thread-cap: 16
 *       queue-cap: 25
 *       ttl-seconds: 60
 *       overrides:
 *         RNDPS:
 *           thread-cap: 12
 *           queue-cap: 15
 * </pre>
 */
@ConfigurationProperties(prefix = "gotechly.plugins.isolation")
public class PluginIsolationProperties {

    /**
     * Off returns every corridor to the shared {@code Schedulers.boundedElastic()} — which is the
     * arrangement that let one provider's timeouts stall every payment type. An escape hatch, not a
     * setting to leave off.
     */
    private boolean enabled = true;

    /** Concurrent in-flight plugin calls allowed per payment type. */
    private int threadCap = 16;

    /**
     * Tasks that may wait for a thread, per payment type.
     *
     * <p><b>Size this against the corridor's own timeout, not intuition.</b> Worst-case wait before
     * a queued request even starts is roughly {@code (queueCap / threadCap) × requestTimeout} — at
     * 25/16 against a 15s RNDPS timeout, about 23 seconds. The shared {@code boundedElastic} queue
     * this replaces holds <b>100,000</b>, which is why a degraded provider produced no backpressure
     * at all: work was accepted and buried instead of refused.
     */
    private int queueCap = 25;

    /** Idle seconds before a pooled thread is reclaimed. */
    private int ttlSeconds = 60;

    /** Per-payment-type overrides, keyed by payment type (e.g. {@code RNDPS}). */
    private Map<String, Override> overrides = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getThreadCap() {
        return threadCap;
    }

    public void setThreadCap(int threadCap) {
        this.threadCap = threadCap;
    }

    public int getQueueCap() {
        return queueCap;
    }

    public void setQueueCap(int queueCap) {
        this.queueCap = queueCap;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public Map<String, Override> getOverrides() {
        return overrides;
    }

    public void setOverrides(Map<String, Override> overrides) {
        this.overrides = overrides;
    }

    public int threadCapFor(String paymentType) {
        Override o = overrides.get(paymentType);
        return o != null && o.getThreadCap() != null ? o.getThreadCap() : threadCap;
    }

    public int queueCapFor(String paymentType) {
        Override o = overrides.get(paymentType);
        return o != null && o.getQueueCap() != null ? o.getQueueCap() : queueCap;
    }

    /** Null fields fall back to the global value, so an override may set only one of the two. */
    public static class Override {

        private Integer threadCap;
        private Integer queueCap;

        public Integer getThreadCap() {
            return threadCap;
        }

        public void setThreadCap(Integer threadCap) {
            this.threadCap = threadCap;
        }

        public Integer getQueueCap() {
            return queueCap;
        }

        public void setQueueCap(Integer queueCap) {
            this.queueCap = queueCap;
        }
    }
}
