package com.dotechly.gotechly.core.config.inward;

import com.dotechly.gotechly.spi.service.inward.InValidationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InValidationHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InValidationHandlerRegistry.class);

    private final Map<String, InValidationRequestHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, InValidationRequestHandler handler) {
        InValidationRequestHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing inward validation handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public InValidationRequestHandler unregister(String paymentType) {
        InValidationRequestHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered inward validation handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public InValidationRequestHandler getHandler(String paymentType) {
        InValidationRequestHandler handler = handlerMap.get(paymentType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No inward validation handler for paymentType: " + paymentType +
                            ". Registered types: " + handlerMap.keySet());
        }
        return handler;
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
