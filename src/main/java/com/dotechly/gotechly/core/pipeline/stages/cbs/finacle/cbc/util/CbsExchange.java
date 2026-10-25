package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util;

import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.jpos.transaction.Context;

/**
 * Carries a {@link PaymentLogSPI} across the jPOS {@link Context} between a CBS stage and its
 * participant, without exposing the object graph to the context dump.
 *
 * <p><b>Why not put the log in the context directly.</b> The stage puts the <em>live</em> pipeline
 * log in the context, the participant mutates it and publishes the same instance back, and the stage
 * thread wakes and carries on into the next pipeline stage. Meanwhile the TransactionManager thread,
 * having finished {@code commit}, calls {@code freeze()} — which dumps every context entry through
 * {@code toString()}. {@code PaymentLogSPI.toString()} walks {@code Details.additionalFields}, a
 * {@code HashMap}, while the next stage (e.g. the RNDPS pacs.008 defaults on a recovery repost) is
 * writing to it: {@code ConcurrentModificationException} inside the TM log. It also wrote the full
 * log — account numbers, names, MSISDNs — into the TM log twice per CBS call.
 *
 * <p>This holder's {@code toString()} prints only the requestId, read once at construction, so the
 * dump never touches the shared object. Identity is preserved: the stage gets back the very
 * instance the participant enriched.
 */
public final class CbsExchange {

    private final PaymentLogSPI log;
    private final String requestId;

    private CbsExchange(PaymentLogSPI log) {
        this.log = log;
        this.requestId = log.getDetails() == null ? null : log.getDetails().getRequestId();
    }

    public static void put(Context ctx, String key, PaymentLogSPI log) {
        ctx.put(key, log == null ? null : new CbsExchange(log));
    }

    public static PaymentLogSPI get(Context ctx, String key) {
        return unwrap(ctx.get(key));
    }

    /** Blocks up to {@code timeoutMs} for the entry, as {@link Context#get(Object, long)} does. */
    public static PaymentLogSPI await(Context ctx, String key, long timeoutMs) {
        return unwrap(ctx.get(key, timeoutMs));
    }

    // A bare PaymentLogSPI is still accepted so a caller not yet on the holder keeps working.
    private static PaymentLogSPI unwrap(Object value) {
        if (value instanceof CbsExchange exchange) {
            return exchange.log;
        }
        return value instanceof PaymentLogSPI log ? log : null;
    }

    @Override
    public String toString() {
        return "PaymentLogSPI[requestId=" + requestId + "]";
    }
}
