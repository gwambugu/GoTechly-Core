package com.dotechly.gotechly.core.config.outward;

import com.dotechly.gotechly.spi.service.outward.OutResultRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OutResultProcessingHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(OutResultProcessingHandlerRegistry.class);

    private final Map<String, OutResultRequestProcessingHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, OutResultRequestProcessingHandler handler) {
        OutResultRequestProcessingHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing result processing handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public OutResultRequestProcessingHandler unregister(String paymentType) {
        OutResultRequestProcessingHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered result processing handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public OutResultRequestProcessingHandler getHandler(String paymentType) {
        OutResultRequestProcessingHandler handler = handlerMap.get(paymentType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No result processing handler for paymentType: " + paymentType +
                            ". Registered types: " + handlerMap.keySet());
        }
        return handler;
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
