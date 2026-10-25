package com.dotechly.gotechly.core.pipeline.stages.outbound;

import com.dotechly.gotechly.core.config.outward.OutPaymentProcessingHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.isolation.PluginSchedulers;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.outward.OutPaymentRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PluginOutPaymentHandlerStage implements PipelineStage  {

    private static final Logger LOG = LoggerFactory.getLogger(PluginOutPaymentHandlerStage.class);

    private final OutPaymentProcessingHandlerRegistry paymentRegistry;
    private final PluginSchedulers schedulers;

    public PluginOutPaymentHandlerStage(OutPaymentProcessingHandlerRegistry paymentRegistry, PluginSchedulers schedulers) {
        this.paymentRegistry = paymentRegistry;
        this.schedulers = schedulers;
    }

    @Override
    public String name() {
        return "out-payment-handler";
    }

    @Override
    public int defaultOrder() {
        return 1000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to outward plugin payment processing handler for paymentType '{}'", paymentType);
        OutPaymentRequestProcessingHandler handler = paymentRegistry.getHandler(paymentType);
        return handler.paymentProcessRequest(request);
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to reactive outward plugin payment processing handler for paymentType '{}'", paymentType);
        OutPaymentRequestProcessingHandler handler = paymentRegistry.getHandler(paymentType);
        return schedulers.isolate(paymentType, handler.paymentProcessRequestReactive(request));
    }
}
