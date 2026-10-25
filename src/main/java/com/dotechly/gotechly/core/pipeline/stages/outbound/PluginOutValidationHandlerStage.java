package com.dotechly.gotechly.core.pipeline.stages.outbound;

import com.dotechly.gotechly.core.config.outward.OutValidationHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.isolation.PluginSchedulers;
import com.dotechly.gotechly.core.pipeline.PipelineStage;


import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.outward.OutValidationRequestHandler;
import com.dotechly.gotechly.spi.service.util.common.PluginHaltException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
/**
 * Pipeline stage that delegates to the plugin's {@link OutValidationRequestHandler}.
 * This stage wraps the existing plugin handler call — positioned wherever
 * "out-validation-handler" appears in the YAML pipeline config.
 */
@Component
public class PluginOutValidationHandlerStage implements PipelineStage  {

    private static final Logger LOG = LoggerFactory.getLogger(PluginOutValidationHandlerStage.class);

    private final OutValidationHandlerRegistry validationRegistry;
    private final PluginSchedulers schedulers;

    public PluginOutValidationHandlerStage(OutValidationHandlerRegistry validationRegistry, PluginSchedulers schedulers) {
        this.validationRegistry = validationRegistry;
        this.schedulers = schedulers;
    }

    @Override
    public String name() {
        return "out-validation-handler";
    }

    @Override
    public int defaultOrder() {
        return 1000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to plugin validation handler for paymentType '{}'", paymentType);
        OutValidationRequestHandler handler = validationRegistry.getRequestValidationHandlerMap(paymentType);
        return handler.validateRequest(request);
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to reactive plugin validation handler for paymentType '{}'", paymentType);
        OutValidationRequestHandler handler = validationRegistry.getRequestValidationHandlerMap(paymentType);
        return schedulers.isolate(paymentType, handler.validateRequestReactive(request))
                // The predicate is what keeps a halt out of this arm. Re-emitting it from an
                // earlier onErrorResume would not work — the error would simply arrive here
                // next — and rewriting a deliberate halt to GN-01 lets db-update and callback
                // run on a pipeline the plugin meant to stop. PipelineExecutor handles it.
                .onErrorResume(ex -> !(ex instanceof PluginHaltException), ex -> {
                    LOG.error("Plugin handler threw unexpected error for '{}': {}",
                            paymentType, ex.getMessage(), ex);
                    request.getDetails().setResponseCode("GN-01");
                    request.getDetails().setResponseDesc("Payment gateway internal error");
                    request.getDetails().setStatus("FAILED");
                    return Mono.just(request);
                });
    }
}