package com.dotechly.gotechly.core.pipeline.stages.inbound;


import com.dotechly.gotechly.core.config.inward.InCreditReversalHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.inward.InwardReversalResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@code in-credit-reversal} — hands an inbound reversal instruction (ISO 20022 pacs.007) to the
 * plugin's {@link InwardReversalResultHandler}.
 *
 * <p>Runs in the inbound {@code credit-reversal} phase, not alongside the credit's stages: a
 * reversal needs a different core-banking operation and acts on an existing transaction, so it gets
 * its own stage list rather than a list whose stages each decide whether the message is theirs.
 */
@Component
public class PluginInCreditReversalStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(PluginInCreditReversalStage.class);

    private final InCreditReversalHandlerRegistry reversalRegistry;

    public PluginInCreditReversalStage(InCreditReversalHandlerRegistry reversalRegistry) {
        this.reversalRegistry = reversalRegistry;
    }

    @Override
    public String name() {
        return "in-credit-reversal";
    }

    @Override
    public int defaultOrder() {
        return 1000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to inward plugin credit-reversal handler for paymentType '{}'", paymentType);
        InwardReversalResultHandler handler = reversalRegistry.getHandler(paymentType);
        return handler.processReversal(request);
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getEffectivePaymentType();
        LOG.debug("Delegating to reactive inward plugin credit-reversal handler for paymentType '{}'",
                paymentType);
        InwardReversalResultHandler handler = reversalRegistry.getHandler(paymentType);
        return handler.processReversalReactive(request);
    }
}
