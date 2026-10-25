package com.dotechly.gotechly.core.config.outward;

import com.dotechly.gotechly.spi.service.outward.OutStatusRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OutStatusProcessingHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(OutStatusProcessingHandlerRegistry.class);

    private final Map<String, OutStatusRequestProcessingHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, OutStatusRequestProcessingHandler handler) {
        OutStatusRequestProcessingHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing outward status processing handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public OutStatusRequestProcessingHandler unregister(String paymentType) {
        OutStatusRequestProcessingHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered outward status processing handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public OutStatusRequestProcessingHandler getHandler(String paymentType) {
        OutStatusRequestProcessingHandler handler = handlerMap.get(paymentType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No outward status processing handler for paymentType: " + paymentType +
                            ". Registered types: " + handlerMap.keySet());
        }
        return handler;
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
