package com.dotechly.gotechly.core.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration for the {@link IdempotencyFilter}.
 *
 * <pre>
 * gotechly:
 *   idempotency:
 *     enabled: true
 *     header-name: Idempotency-Key
 *     path-prefixes:
 *       - /payment/api/
 * </pre>
 */
@ConfigurationProperties(prefix = "gotechly.idempotency")
public class IdempotencyProperties {

    /** Master switch. When false the filter passes every request straight through. */
    private boolean enabled = true;

    /** Header the client sends to opt a POST into dedup. */
    private String headerName = "Idempotency-Key";

    /** Only POSTs whose path starts with one of these prefixes are deduped. */
    private List<String> pathPrefixes = List.of("/payment/api/");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public List<String> getPathPrefixes() {
        return pathPrefixes;
    }

    public void setPathPrefixes(List<String> pathPrefixes) {
        this.pathPrefixes = pathPrefixes;
    }
}
