package com.dotechly.gotechly.core.config.outward;

import com.dotechly.gotechly.spi.service.outward.OutValidationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OutValidationHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(OutValidationHandlerRegistry.class);

    private final Map<String, OutValidationRequestHandler> handlerMap = new ConcurrentHashMap<>();

    /**
     * Register a handler for a given payment type.
     * If a handler already exists for this payment type, it is replaced and a warning is logged.
     */
    public void register(String paymentType, OutValidationRequestHandler handler) {
        OutValidationRequestHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    /**
     * Retrieve the handler for a given payment type.
     *
     * @throws IllegalArgumentException if no handler is registered for the payment type.
     */
    /**
     * Remove the handler for the given payment type.
     * Returns the removed handler, or null if none was registered.
     */
    public OutValidationRequestHandler unregister(String paymentType) {
        OutValidationRequestHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered validation handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public OutValidationRequestHandler getRequestValidationHandlerMap(String paymentType) {
        OutValidationRequestHandler handler = handlerMap.get(paymentType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "Unsupported paymentType: " + paymentType +
                            ". Registered types: " + handlerMap.keySet());
        }
        return handler;
    }

    /**
     * Returns the set of currently registered payment types (for logging/diagnostics).
     */
    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
