package com.dotechly.gotechly.core.config.inward;

import com.dotechly.gotechly.spi.service.inward.InStatusRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InStatusProcessingHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InStatusProcessingHandlerRegistry.class);

    private final Map<String, InStatusRequestProcessingHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, InStatusRequestProcessingHandler handler) {
        InStatusRequestProcessingHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing inward status processing handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public InStatusRequestProcessingHandler unregister(String paymentType) {
        InStatusRequestProcessingHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered inward status processing handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public InStatusRequestProcessingHandler getHandler(String paymentType) {
        InStatusRequestProcessingHandler handler = handlerMap.get(paymentType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No inward status processing handler for paymentType: " + paymentType +
                            ". Registered types: " + handlerMap.keySet());
        }
        return handler;
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
