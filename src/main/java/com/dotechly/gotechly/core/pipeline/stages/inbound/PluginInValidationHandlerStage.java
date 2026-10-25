package com.dotechly.gotechly.core.pipeline.stages.inbound;

import com.dotechly.gotechly.core.config.inward.InValidationHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.isolation.PluginSchedulers;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.inward.InValidationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Pipeline stage that delegates to the plugin's {@link InValidationRequestHandler}.
 * Positioned wherever "in-validation-handler" appears in the YAML pipeline config.
 */
@Component
public class PluginInValidationHandlerStage implements PipelineStage  {

    private static final Logger LOG = LoggerFactory.getLogger(PluginInValidationHandlerStage.class);

    private final InValidationHandlerRegistry validationRegistry;
    private final PluginSchedulers schedulers;

    public PluginInValidationHandlerStage(InValidationHandlerRegistry validationRegistry, PluginSchedulers schedulers) {
        this.validationRegistry = validationRegistry;
        this.schedulers = schedulers;
    }

    @Override
    public String name() {
        return "in-validation-handler";
    }

    @Override
    public int defaultOrder() {
        return 1000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to inward plugin validation handler for paymentType '{}'", paymentType);
        InValidationRequestHandler handler = validationRegistry.getHandler(paymentType);
        return handler.validateRequest(request);
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to reactive inward plugin validation handler for paymentType '{}'", paymentType);
        InValidationRequestHandler handler = validationRegistry.getHandler(paymentType);
        return schedulers.isolate(paymentType, handler.validateRequestReactive(request));
    }
}
