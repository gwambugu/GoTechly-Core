package com.dotechly.gotechly.core.config.outward;


import com.dotechly.gotechly.spi.service.outward.account.LinkedAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link LinkedAccount} plugins keyed by paymentType — the account/alias (proxy)
 * management capability. Mirrors {@link DarajaInquiryHandlerRegistry} and the phase handler
 * registries; populated by {@code PluginLoader} via ServiceLoader and kept in sync on hot-reload.
 */
@Component
public class LinkedAccountRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(LinkedAccountRegistry.class);

    private final Map<String, LinkedAccount> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, LinkedAccount handler) {
        LinkedAccount existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing linked-account handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public LinkedAccount unregister(String paymentType) {
        LinkedAccount removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered linked-account handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public Optional<LinkedAccount> findByPaymentType(String paymentType) {
        return paymentType == null ? Optional.empty()
                : Optional.ofNullable(handlerMap.get(paymentType));
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
