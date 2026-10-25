package com.dotechly.gotechly.core.pipeline.stages.inbound;

import com.dotechly.gotechly.core.config.inward.InResultProcessingHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.isolation.PluginSchedulers;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.dto.payment.ResultProcessingOutcome;
import com.dotechly.gotechly.spi.service.inward.InResultRequestProcessingHandler;
import com.dotechly.gotechly.spi.service.util.common.PluginHaltException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@code in-result-handler} — parses a result callback the provider sent <em>to us</em>, whose raw
 * body arrives on {@code context.getRawResult()}.
 *
 * <p>The opposite direction of travel is {@code in-callback-handler}
 * ({@link com.dotechly.gotechly.spi.service.inward.InwardCallback}), which composes and sends
 * a report about an inbound transaction to the scheme.
 */
@Component
public class PluginInResultHandlerStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(PluginInResultHandlerStage.class);

    private final InResultProcessingHandlerRegistry resultRegistry;
    private final PluginSchedulers schedulers;

    public PluginInResultHandlerStage(InResultProcessingHandlerRegistry resultRegistry, PluginSchedulers schedulers) {
        this.resultRegistry = resultRegistry;
        this.schedulers = schedulers;
    }

    @Override
    public String name() {
        return "in-result-handler";
    }

    @Override
    public int defaultOrder() {
        return 1000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        String rawResult = context.getRawResult();
        LOG.debug("Delegating to inward plugin result processing handler for paymentType '{}'", paymentType);
        InResultRequestProcessingHandler handler = resultRegistry.getHandler(paymentType);
        ResultProcessingOutcome outcome;
        try {
            outcome = handler.processResult(rawResult);
        } catch (PluginHaltException halt) {
            stampProviderResponse(context, halt);
            throw halt;
        }
        context.setRawResultResponse(outcome.getProviderResponse());
        return outcome.getPaymentLog();
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        String rawResult = context.getRawResult();
        LOG.debug("Delegating to reactive inward plugin result processing handler for paymentType '{}'", paymentType);
        InResultRequestProcessingHandler handler = resultRegistry.getHandler(paymentType);
        return schedulers.isolate(paymentType, handler.processResultReactive(rawResult))
                .doOnError(PluginHaltException.class, halt -> stampProviderResponse(context, halt))
                .map(outcome -> {
                    context.setRawResultResponse(outcome.getProviderResponse());
                    return outcome.getPaymentLog();
                });
    }

    /**
     * Carries the provider acknowledgement off a halting handler.
     *
     * <p>A result handler normally supplies it through {@link ResultProcessingOutcome}, which a
     * halt never returns — and {@code ReactiveResultProcessingService} answers the provider with
     * the empty string when nothing set it, which a provider that retries on a bad acknowledgement
     * reads as "not delivered" and replays. So a handler that halts can put the body it wants sent
     * on the exception, and it is stamped here before the halt propagates.
     *
     * <p>A halt with no provider response leaves the field alone rather than blanking it: an
     * earlier stage may legitimately have set one.
     */
    private static void stampProviderResponse(PipelineContext context, PluginHaltException halt) {
        if (halt.getProviderResponse() != null) {
            context.setRawResultResponse(halt.getProviderResponse());
        } else {
            LOG.warn("Result handler halted without a provider response (haltCode={}); "
                    + "the provider will be acknowledged with an empty body", halt.getHaltCode());
        }
    }
}
