package com.dotechly.gotechly.core.pipeline;

import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import reactor.core.publisher.Mono;

/**
 * Wraps a {@link PipelineStage} to temporarily set a delegate payment type
 * in the {@link PipelineContext} before execution, and clears it afterward.
 * <p>
 * Created by {@link PipelineRegistry} when a stage name uses the delegate syntax:
 * {@code out-async-validation-handler>MPESA}
 * <p>
 * This allows composite payment types (e.g., IMT-MPESA) to reuse handler stages
 * registered by other payment types (e.g., MPESA) without implementing their own handlers.
 */
class DelegatingStageWrapper implements PipelineStage {

    private final PipelineStage delegate;
    private final String delegatePaymentType;
    private final String displayName;

    DelegatingStageWrapper(PipelineStage delegate, String delegatePaymentType) {
        this.delegate = delegate;
        this.delegatePaymentType = delegatePaymentType;
        this.displayName = delegate.name() + ">" + delegatePaymentType;
    }

    @Override
    public String name() {
        return displayName;
    }

    @Override
    public int defaultOrder() {
        return delegate.defaultOrder();
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        context.setAttribute("delegatePaymentType", delegatePaymentType);
        try {
            return delegate.execute(context, request);
        } finally {
            context.setAttribute("delegatePaymentType", null);
        }
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        context.setAttribute("delegatePaymentType", delegatePaymentType);
        return delegate.executeReactive(context, request)
                .doFinally(signal -> context.setAttribute("delegatePaymentType", null));
    }
}
