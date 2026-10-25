package com.dotechly.gotechly.core.config;

import com.dotechly.gotechly.spi.service.PluginContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Core's implementation of PluginContext.
 * Holds shared services (WebClient, etc.) and per-plugin configuration properties.
 * PluginLoader creates one instance per plugin handler with that plugin's config.
 */
public class DefaultPluginContext implements PluginContext {

    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Map<String, String> properties;

    public DefaultPluginContext() {
        this.properties = Collections.emptyMap();
    }

    public DefaultPluginContext(Map<String, String> properties) {
        this.properties = properties != null
                ? Collections.unmodifiableMap(new HashMap<>(properties))
                : Collections.emptyMap();
    }

    /**
     * Register a service to be available to plugins.
     */
    public <T> void registerService(Class<T> type, T instance) {
        services.put(type, instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        T service = (T) services.get(serviceClass);
        if (service == null) {
            throw new IllegalArgumentException(
                    "No service registered for type: " + serviceClass.getName() +
                            ". Available services: " + services.keySet());
        }
        return service;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }
}
