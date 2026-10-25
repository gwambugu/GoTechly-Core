package com.dotechly.gotechly.core.config;

import java.util.List;
import java.util.Set;

/**
 * Metadata about a loaded plugin.
 * Stored internally by PluginLoader; exposed via the admin endpoint.
 * <p>
 * For JAR-based plugins, {@code jarFileName} is the JAR file name and {@code configOnly} is false.
 * For config-only (composite) payment types, {@code jarFileName} is the config file name and {@code configOnly} is true.
 */
public record PluginInfo(
        String jarFileName,
        Set<String> paymentTypes,
        List<HandlerInfo> handlers,
        boolean configOnly
) {
    /** Backward-compatible constructor for JAR-based plugins. */
    public PluginInfo(String jarFileName, Set<String> paymentTypes, List<HandlerInfo> handlers) {
        this(jarFileName, paymentTypes, handlers, false);
    }

    /**
     * Metadata about a single handler discovered from a plugin JAR,
     * or a delegated handler stage for config-only payment types.
     */
    public record HandlerInfo(
            String paymentType,
            String handlerClassName,
            String handlerType,
            String version,
            String mode
    ) {}
}
