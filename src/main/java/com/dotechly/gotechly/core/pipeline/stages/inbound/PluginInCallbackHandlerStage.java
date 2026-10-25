package com.dotechly.gotechly.core.pipeline.stages.inbound;


import com.dotechly.gotechly.core.config.inward.InCallbackHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.isolation.PluginSchedulers;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.inward.InwardCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@code in-callback-handler} — reports an inbound transaction's outcome back to the scheme that
 * sent it, through the plugin's {@link InwardCallback}.
 *
 * <p>Belongs at the end of an inbound request phase, after the stage that decides the outcome, and
 * in that phase's {@code compensation:} block for the halt codes that stage can raise — a halted
 * pipeline skips its remaining stages, which would leave the scheme uninformed in precisely the
 * case where the answer is a rejection.
 *
 * <p>Distinct from Core's {@code callback} stage: that one notifies the originating <b>channel</b>
 * from a Freemarker template, this one answers the <b>scheme</b> with a message only the plugin can
 * compose (typically signed, and protocol-specific).
 */
@Component
public class PluginInCallbackHandlerStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(PluginInCallbackHandlerStage.class);

    private final InCallbackHandlerRegistry callbackRegistry;
    private final PluginSchedulers schedulers;

    public PluginInCallbackHandlerStage(InCallbackHandlerRegistry callbackRegistry, PluginSchedulers schedulers) {
        this.callbackRegistry = callbackRegistry;
        this.schedulers = schedulers;
    }

    @Override
    public String name() {
        return "in-callback-handler";
    }

    @Override
    public int defaultOrder() {
        return 4000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to inward plugin callback handler for paymentType '{}'", paymentType);
        InwardCallback handler = callbackRegistry.getHandler(paymentType);
        return orRequest(handler.sendCallback(request), request);
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to reactive inward plugin callback handler for paymentType '{}'", paymentType);
        InwardCallback handler = callbackRegistry.getHandler(paymentType);
        return schedulers.isolate(paymentType, handler.sendCallbackReactive(request)).map(result -> orRequest(result, request));
    }

    /** A handler that reported an outcome has nothing new to return; never blank the transaction. */
    private static PaymentLogSPI orRequest(PaymentLogSPI result, PaymentLogSPI request) {
        return result == null ? request : result;
    }
}
