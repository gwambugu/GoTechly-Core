package com.dotechly.gotechly.core.pipeline.stages.outbound;

import com.dotechly.gotechly.core.config.outward.OutStatusProcessingHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.isolation.PluginSchedulers;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.outward.OutStatusRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PluginOutStatusHandlerStage implements PipelineStage  {

    private static final Logger LOG = LoggerFactory.getLogger(PluginOutStatusHandlerStage.class);

    private final OutStatusProcessingHandlerRegistry statusRegistry;
    private final PluginSchedulers schedulers;

    public PluginOutStatusHandlerStage(OutStatusProcessingHandlerRegistry statusRegistry, PluginSchedulers schedulers) {
        this.statusRegistry = statusRegistry;
        this.schedulers = schedulers;
    }

    @Override
    public String name() {
        return "out-status-handler";
    }

    @Override
    public int defaultOrder() {
        return 1000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to outward plugin status processing handler for paymentType '{}'", paymentType);
        OutStatusRequestProcessingHandler handler = statusRegistry.getHandler(paymentType);
        return handler.queryStatus(request);
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to reactive outward plugin status processing handler for paymentType '{}'", paymentType);
        OutStatusRequestProcessingHandler handler = statusRegistry.getHandler(paymentType);
        return schedulers.isolate(paymentType, handler.queryStatusReactive(request));
    }
}
