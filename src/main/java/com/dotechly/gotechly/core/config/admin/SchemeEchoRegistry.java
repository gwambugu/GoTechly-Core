package com.dotechly.gotechly.core.config.admin;

import com.dotechly.gotechly.spi.service.admin.SchemeEchoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link SchemeEchoHandler} plugins keyed by paymentType. Mirrors the other capability
 * registries (e.g. {@code LinkedAccountRegistry}, {@code LinkedWalletBankAccountRegistry});
 * populated by {@code PluginLoader} via ServiceLoader and kept in sync on hot-reload.
 */
@Component
public class SchemeEchoRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SchemeEchoRegistry.class);

    private final Map<String, SchemeEchoHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, SchemeEchoHandler handler) {
        SchemeEchoHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing scheme-echo handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public SchemeEchoHandler unregister(String paymentType) {
        SchemeEchoHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered scheme-echo handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public Optional<SchemeEchoHandler> findByPaymentType(String paymentType) {
        return Optional.ofNullable(handlerMap.get(paymentType));
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
