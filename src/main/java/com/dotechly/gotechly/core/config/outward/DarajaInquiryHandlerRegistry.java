package com.dotechly.gotechly.core.config.outward;

import com.dotechly.gotechly.spi.service.outward.inquiry.DarajaInquiryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DarajaInquiryHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(DarajaInquiryHandlerRegistry.class);

    private final Map<String, DarajaInquiryHandler> handlerMap = new ConcurrentHashMap<>();

    public void register(String paymentType, DarajaInquiryHandler handler) {
        DarajaInquiryHandler existing = handlerMap.put(paymentType, handler);
        if (existing != null) {
            LOG.warn("Replaced existing Daraja inquiry handler for paymentType '{}': {} -> {}",
                    paymentType, existing.getClass().getName(), handler.getClass().getName());
        }
    }

    public DarajaInquiryHandler unregister(String paymentType) {
        DarajaInquiryHandler removed = handlerMap.remove(paymentType);
        if (removed != null) {
            LOG.info("Unregistered Daraja inquiry handler for paymentType '{}': {}",
                    paymentType, removed.getClass().getName());
        }
        return removed;
    }

    public Optional<DarajaInquiryHandler> findByPaymentType(String paymentType) {
        return Optional.ofNullable(handlerMap.get(paymentType));
    }

    public Set<String> getRegisteredPaymentTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }
}
