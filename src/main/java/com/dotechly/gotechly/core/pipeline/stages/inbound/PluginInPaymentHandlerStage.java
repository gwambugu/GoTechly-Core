package com.dotechly.gotechly.core.pipeline.stages.inbound;

import com.dotechly.gotechly.core.config.inward.InPaymentProcessingHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.isolation.PluginSchedulers;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.inward.InPaymentRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PluginInPaymentHandlerStage implements PipelineStage  {

    private static final Logger LOG = LoggerFactory.getLogger(PluginInPaymentHandlerStage.class);

    private final InPaymentProcessingHandlerRegistry paymentRegistry;
    private final PluginSchedulers schedulers;

    public PluginInPaymentHandlerStage(InPaymentProcessingHandlerRegistry paymentRegistry, PluginSchedulers schedulers) {
        this.paymentRegistry = paymentRegistry;
        this.schedulers = schedulers;
    }

    @Override
    public String name() {
        return "in-payment-handler";
    }

    @Override
    public int defaultOrder() {
        return 1000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to inward plugin payment processing handler for paymentType '{}'", paymentType);
        InPaymentRequestProcessingHandler handler = paymentRegistry.getHandler(paymentType);
        return handler.paymentProcessRequest(request);
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to reactive inward plugin payment processing handler for paymentType '{}'", paymentType);
        InPaymentRequestProcessingHandler handler = paymentRegistry.getHandler(paymentType);
        return schedulers.isolate(paymentType, handler.paymentProcessRequestReactive(request));
    }
}
