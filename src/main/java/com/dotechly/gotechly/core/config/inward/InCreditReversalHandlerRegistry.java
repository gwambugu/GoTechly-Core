package com.dotechly.gotechly.core.config.inward;


import com.dotechly.gotechly.spi.service.inward.InwardReversalResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Registry of plugin handlers for inbound credit-reversal instructions (ISO 20022 pacs.007). */
@Component
public class InCreditReversalHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InCreditReversalHandlerRegistry.class);

    private final Map<String, InwardReversalResultHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, InwardReversalResultHandler handler) {
        InwardReversalResultHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing inward credit-reversal handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public InwardReversalResultHandler unregister(String paymentType) {
        InwardReversalResultHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered inward credit-reversal handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public InwardReversalResultHandler getHandler(String paymentType) {
        InwardReversalResultHandler handler = handlerMap.get(paymentType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No inward credit-reversal handler for paymentType: " + paymentType +
                            ". Registered types: " + handlerMap.keySet());
        }
        return handler;
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
