package com.dotechly.gotechly.core.pipeline.isolation;

/**
 * One payment type's bulkhead is full: its threads are all busy and its queue is at capacity, so
 * further work for that corridor is refused rather than queued.
 *
 * <p>This is load shedding working, not a defect. It is thrown in preference to letting the call
 * wait, because waiting is what turned one provider's degradation into a platform-wide stall: an
 * unbounded queue accepts everything and fails nothing until every thread in the process is held.
 *
 * <p>Callers should treat it as a retryable, corridor-specific failure — the transaction has not
 * been sent to the provider. A sustained rate of these means the provider is unhealthy or the caps
 * in {@link PluginIsolationProperties} are too small for the corridor's real traffic; check the
 * provider first, since raising the caps to cover a hanging endpoint just rebuilds the original
 * failure more slowly.
 */
public class PluginOverloadedException extends RuntimeException {

    private final String paymentType;

    public PluginOverloadedException(String paymentType, Throwable cause) {
        super("Payment type '" + paymentType + "' is saturated; work refused rather than queued. "
                + "The request was not sent to the provider and may be retried.", cause);
        this.paymentType = paymentType;
    }

    public String getPaymentType() {
        return paymentType;
    }
}
