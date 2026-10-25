package com.dotechly.gotechly.core.config.outward;


import com.dotechly.gotechly.spi.service.outward.OutPaymentRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OutPaymentProcessingHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(OutPaymentProcessingHandlerRegistry.class);

    private final Map<String, OutPaymentRequestProcessingHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, OutPaymentRequestProcessingHandler handler) {
        OutPaymentRequestProcessingHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing payment processing handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public OutPaymentRequestProcessingHandler unregister(String paymentType) {
        OutPaymentRequestProcessingHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered payment processing handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public OutPaymentRequestProcessingHandler getHandler(String paymentType) {
        OutPaymentRequestProcessingHandler handler = handlerMap.get(paymentType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No payment processing handler for paymentType: " + paymentType +
                            ". Registered types: " + handlerMap.keySet());
        }
        return handler;
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
