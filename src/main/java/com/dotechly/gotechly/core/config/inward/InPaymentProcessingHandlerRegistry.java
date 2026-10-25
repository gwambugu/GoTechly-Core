package com.dotechly.gotechly.core.config.inward;

import com.dotechly.gotechly.spi.service.inward.InPaymentRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InPaymentProcessingHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InPaymentProcessingHandlerRegistry.class);

    private final Map<String, InPaymentRequestProcessingHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, InPaymentRequestProcessingHandler handler) {
        InPaymentRequestProcessingHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing inward payment processing handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public InPaymentRequestProcessingHandler unregister(String paymentType) {
        InPaymentRequestProcessingHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered inward payment processing handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public InPaymentRequestProcessingHandler getHandler(String paymentType) {
        InPaymentRequestProcessingHandler handler = handlerMap.get(paymentType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No inward payment processing handler for paymentType: " + paymentType +
                            ". Registered types: " + handlerMap.keySet());
        }
        return handler;
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
