package com.dotechly.gotechly.core.config.iso;


import com.dotechly.gotechly.spi.service.iso.Iso8583Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link Iso8583Plugin} plugins, keyed by payment type. Mirrors the
 * {@code DarajaInquiryHandlerRegistry} pattern: a plain in-memory map populated by
 * {@code PluginLoader} on JAR load and cleared on unload.
 * <p>
 * These plugins serve inbound ISO 8583 requests over TCP via their own jPOS deploy XML; this
 * registry exists only for the standard plugin-registration ceremony (lifecycle + portal
 * visibility), not for request routing. Core holds no jPOS state on their behalf — the plugin
 * publishes what its jPOS components need into {@code NameRegistrar} from its own {@code init}.
 */
@Component
public class Iso8583PluginRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(Iso8583PluginRegistry.class);

    private final Map<String, Iso8583Plugin> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, Iso8583Plugin handler) {
        Iso8583Plugin existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing ISO 8583 plugin for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public Iso8583Plugin unregister(String paymentType) {
        Iso8583Plugin removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered ISO 8583 plugin for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public Optional<Iso8583Plugin> findByPaymentType(String paymentType) {
        return Optional.ofNullable(handlerMap.get(paymentType));
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
