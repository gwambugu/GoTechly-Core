package com.dotechly.gotechly.core.config.inward;


import com.dotechly.gotechly.spi.service.inward.linkedwallet.LinkedWalletBankAccountHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link LinkedWalletBankAccountHandler} plugins keyed by paymentType. Mirrors the
 * other handler registries (e.g. {@code InValidationHandlerRegistry},
 * {@code DarajaInquiryHandlerRegistry}); populated by {@code PluginLoader} via ServiceLoader and
 * kept in sync on hot-reload.
 */
@Component
public class LinkedWalletBankAccountRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(LinkedWalletBankAccountRegistry.class);

    private final Map<String, LinkedWalletBankAccountHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, LinkedWalletBankAccountHandler handler) {
        LinkedWalletBankAccountHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing linked-wallet handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public LinkedWalletBankAccountHandler unregister(String paymentType) {
        LinkedWalletBankAccountHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered linked-wallet handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public Optional<LinkedWalletBankAccountHandler> findByPaymentType(String paymentType) {
        return Optional.ofNullable(handlerMap.get(paymentType));
    }

    /**
     * Returns the sole registered handler when exactly one is present, else empty. Lets the
     * inbound endpoint dispatch without a configured paymentType when only one bank plugin is
     * deployed.
     */
    public Optional<LinkedWalletBankAccountHandler> findSole() {
        if (handlerMap.size() == 1) {
            return handlerMap.values().stream().findFirst();
        }
        return Optional.empty();
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
