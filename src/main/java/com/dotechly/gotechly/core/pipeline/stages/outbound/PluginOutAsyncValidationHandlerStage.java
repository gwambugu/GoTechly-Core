package com.dotechly.gotechly.core.pipeline.stages.outbound;

import com.dotechly.gotechly.core.config.outward.OutValidationHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.isolation.PluginSchedulers;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.outward.OutValidationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Pipeline stage that delegates to the plugin's async validation handler.
 * Calls {@link OutValidationRequestHandler#validateRequestAsync(PaymentLogSPI)},
 * which queues work for asynchronous processing and returns an acknowledgment string.
 * The acknowledgment is stored in the pipeline context for the REST layer to extract.
 */
@Component
public class PluginOutAsyncValidationHandlerStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(PluginOutAsyncValidationHandlerStage.class);

    private final OutValidationHandlerRegistry validationRegistry;
    private final PluginSchedulers schedulers;

    public PluginOutAsyncValidationHandlerStage(OutValidationHandlerRegistry validationRegistry, PluginSchedulers schedulers) {
        this.validationRegistry = validationRegistry;
        this.schedulers = schedulers;
    }

    @Override
    public String name() {
        return "out-async-validation-handler";
    }

    @Override
    public int defaultOrder() {
        return 1000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to async plugin validation handler for paymentType '{}'", paymentType);

        OutValidationRequestHandler handler = validationRegistry.getRequestValidationHandlerMap(paymentType);
        String ackResponse = handler.validateRequestAsync(request);

        context.setAttribute("async-response", ackResponse);
        request.getDetails().setStatus("PENDING");
        request.getDetails().setApiMode("ASYNC");

        LOG.debug("Async validation handler for '{}' returned: {}", paymentType, ackResponse);
        return request;
    }

    /**
     * Overrides the interface default purely to swap the scheduler. The default offloads to the
     * SHARED {@code boundedElastic}, which is the pool one degraded provider drained; this stage
     * blocks on the provider exactly like the others, so it belongs in the same per-corridor
     * bulkhead.
     */
    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        return schedulers.isolate(paymentType, Mono.fromCallable(() -> execute(context, request)));
    }
}
