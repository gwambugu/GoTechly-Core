package com.dotechly.gotechly.core.config.inward;

import com.dotechly.gotechly.spi.service.inward.InResultRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InResultProcessingHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InResultProcessingHandlerRegistry.class);

    private final Map<String, InResultRequestProcessingHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, InResultRequestProcessingHandler handler) {
        InResultRequestProcessingHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing inward result processing handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public InResultRequestProcessingHandler unregister(String paymentType) {
        InResultRequestProcessingHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered inward result processing handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public InResultRequestProcessingHandler getHandler(String paymentType) {
        InResultRequestProcessingHandler handler = handlerMap.get(paymentType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No inward result processing handler for paymentType: " + paymentType +
                            ". Registered types: " + handlerMap.keySet());
        }
        return handler;
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
