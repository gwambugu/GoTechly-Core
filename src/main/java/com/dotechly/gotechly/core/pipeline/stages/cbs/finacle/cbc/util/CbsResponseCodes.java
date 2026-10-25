package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util;

import java.util.Set;

/**
 * Reads Finacle's DE39 as one of three verdicts.
 *
 * <p>Every Finacle touchpoint has to answer the same question — <i>did it work, did it definitely
 * not work, or do we not know?</i> — and before this class each of them answered it separately, with
 * {@code "000"} / {@code "911"} / {@code else} written out by hand. Adding a code meant finding all
 * of them, and the copies had already drifted.
 *
 * <p><b>The third verdict is the one that matters.</b> On the two <em>mutating</em> touchpoints
 * (posting, reversal) a host timeout is not a decline: the request reached Finacle and may have been
 * applied, so the entry may be on the books. Reading it as a decline tells the scheme the transfer
 * was rejected <em>and</em> — because the halt code is what selects the compensation — routes it
 * down the path with no {@code deferred-recovery} stage, so nothing ever goes back to ask. That is a
 * silent, permanent inconsistency, and it is exactly what
 * {@code TransactionPaymentStage}/{@code cbc-payment-unconfirmed} exists to prevent.
 *
 * <p>On the two <em>inquiry</em> touchpoints (account validation, transaction status) a timeout
 * carries no such risk — nothing changed state, so there is nothing to reconcile — but it is still
 * not an answer, and must not be reported as one.
 *
 * <p>An <b>absent</b> code is {@link Verdict#TIMEOUT}, never {@link Verdict#DECLINED}: a MUX timeout
 * leaves {@code CBS.respCode} null, and reading that empty string as "some code other than success"
 * is the original form of this bug.
 *
 * <p>An <b>unrecognised</b> code is {@link Verdict#DECLINED}. That asymmetry is deliberate: the
 * timeout set is closed and known, whereas Finacle's decline codes are open-ended and each one means
 * the host looked at the request and refused it. Treating an unknown code as a timeout instead would
 * raise a recovery row for every new business decline Finacle ever adds.
 */
public final class CbsResponseCodes {

    /** Finacle's success code. */
    public static final String SUCCESS_CODE = "000";

    /**
     * The host's internal timeout codes.
     *
     * <p>All four mean the same thing operationally — Finacle did not answer within its own window —
     * so all four leave a posting or a reversal in an unknown state. {@code 911} was the only one
     * recognised here until 2026-08-31; {@code 906}, {@code 907} and {@code 909} were falling through
     * to {@link Verdict#DECLINED}.
     */
    private static final Set<String> TIMEOUT_CODES = Set.of("911", "906", "907", "909");

    private CbsResponseCodes() {
    }

    /** What Finacle's DE39 says about the request it answers. */
    public enum Verdict {
        /** {@code 000}. The host applied the request. */
        SUCCESS,
        /**
         * No answer, or the host saying it ran out of time. Says nothing about whether the request was
         * applied — on a mutating touchpoint the caller must reconcile rather than conclude.
         */
        TIMEOUT,
        /** The host looked at the request and refused it. Definitive: nothing was applied. */
        DECLINED
    }

    /**
     * Classifies a DE39 value. Tolerates null, blank and padded input — the field arrives straight
     * off an {@code ISOMsg} and is not always trimmed.
     */
    public static Verdict classify(String de39) {
        if (de39 == null || de39.isBlank()) {
            return Verdict.TIMEOUT;
        }
        String code = de39.trim();
        if (SUCCESS_CODE.equals(code)) {
            return Verdict.SUCCESS;
        }
        return TIMEOUT_CODES.contains(code) ? Verdict.TIMEOUT : Verdict.DECLINED;
    }

    /** True when {@code de39} is one of the host's timeout codes. Excludes the absent-code case. */
    public static boolean isTimeoutCode(String de39) {
        return de39 != null && TIMEOUT_CODES.contains(de39.trim());
    }
}
