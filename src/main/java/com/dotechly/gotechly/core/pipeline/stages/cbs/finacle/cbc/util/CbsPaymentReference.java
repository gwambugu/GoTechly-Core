package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util;

import com.dotechly.gotechly.spi.service.dto.payment.CBS;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;

import java.util.Map;

/**
 * Resolves the reference a Finacle transaction-status inquiry is keyed on.
 *
 * <p>Two references identify one posting, and which of them exists depends on how the payment ended:
 *
 * <ol>
 *   <li><b>{@code CBS.tranRef}</b> — the core-banking reference lifted from field 127 of the payment
 *       <em>response</em>. Present whenever Finacle answered at all, and the preferred key.</li>
 *   <li><b>{@code cbc.payment.request.DE37}</b> — field 37 (the RRN) of the ISO request we sent.
 *       On a clear timeout no response ever arrives, so there is no {@code tranRef}, and this is the
 *       only handle on the transaction that may or may not have posted. It is stamped by
 *       {@code TransactionPaymentParticipant.setAdditionalFieldsFromRequest} before the message goes
 *       out, so it survives precisely the failure that destroys the other one.</li>
 * </ol>
 *
 * <p><b>There is deliberately no third fallback.</b> The previous code fell back to
 * {@code Details.requestId}, which Finacle has never seen: the inquiry then comes back "not found"
 * and that answer is <em>definitive</em> — the corridor declares a possibly-posted transaction
 * failed and reverses nothing. When neither reference is available the caller must halt as
 * unconfirmed instead of guessing, which is why {@link #isPresent(PaymentLogSPI)} exists.
 */
public final class CbsPaymentReference {

    /** Key under which the payment request's field 37 (RRN) is stamped on {@code CBS.additionalFields}. */
    public static final String PAYMENT_REQUEST_DE37 = "cbc.payment.request.DE37";

    private CbsPaymentReference() {
    }

    /**
     * @return the inquiry key, or {@code null} when the transaction carries neither reference.
     */
    public static String resolve(PaymentLogSPI paymentLog) {
        CBS cbs = paymentLog == null ? null : paymentLog.getCbs();
        if (cbs == null) {
            return null;
        }
        // tranRef arrives as "id|extra|extra"; Finacle wants the leading segment.
        String tranRef = trimToNull(cbs.getTranRef());
        if (tranRef != null) {
            return tranRef.contains("|") ? trimToNull(tranRef.split("\\|")[0]) : tranRef;
        }
        Map<String, String> additionalFields = cbs.getAdditionalFields();
        if (additionalFields != null) {
            return trimToNull(additionalFields.get(PAYMENT_REQUEST_DE37));
        }
        return null;
    }

    /** Whether an inquiry can be keyed at all. */
    public static boolean isPresent(PaymentLogSPI paymentLog) {
        return resolve(paymentLog) != null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
