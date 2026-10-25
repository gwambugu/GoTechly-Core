package com.dotechly.gotechly.core.pipeline.stages.inbound;

import com.dotechly.gotechly.core.config.inward.InStatusProcessingHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.isolation.PluginSchedulers;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.inward.InStatusRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PluginInStatusHandlerStage implements PipelineStage   {

    private static final Logger LOG = LoggerFactory.getLogger(PluginInStatusHandlerStage.class);

    private final InStatusProcessingHandlerRegistry statusRegistry;
    private final PluginSchedulers schedulers;

    public PluginInStatusHandlerStage(InStatusProcessingHandlerRegistry statusRegistry, PluginSchedulers schedulers) {
        this.statusRegistry = statusRegistry;
        this.schedulers = schedulers;
    }

    @Override
    public String name() {
        return "in-status-handler";
    }

    @Override
    public int defaultOrder() {
        return 1000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to inward plugin status processing handler for paymentType '{}'", paymentType);
        InStatusRequestProcessingHandler handler = statusRegistry.getHandler(paymentType);
        return handler.processResult(request);
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to reactive inward plugin status processing handler for paymentType '{}'", paymentType);
        InStatusRequestProcessingHandler handler = statusRegistry.getHandler(paymentType);
        return schedulers.isolate(paymentType, handler.processResultReactive(request));
    }
}
