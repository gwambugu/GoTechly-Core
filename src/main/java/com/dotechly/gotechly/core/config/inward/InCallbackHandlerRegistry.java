package com.dotechly.gotechly.core.config.inward;


import com.dotechly.gotechly.spi.service.inward.InwardCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Registry of plugin handlers that report an inbound transaction's outcome back to the scheme. */
@Component
public class InCallbackHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InCallbackHandlerRegistry.class);

    private final Map<String, InwardCallback> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, InwardCallback handler) {
        InwardCallback existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing inward callback handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public InwardCallback unregister(String paymentType) {
        InwardCallback removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered inward callback handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public InwardCallback getHandler(String paymentType) {
        InwardCallback handler = handlerMap.get(paymentType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No inward callback handler for paymentType: " + paymentType +
                            ". Registered types: " + handlerMap.keySet());
        }
        return handler;
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
