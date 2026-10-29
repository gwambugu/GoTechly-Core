package com.dotechly.gotechly.core.pipeline.stages.callback;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@code dfs-credit-status} — the reconciler's gate for a credit held in a <b>DFS wallet</b>.
 *
 * <p>The ordinary gate, {@code cbc-txn-status}, asks core banking — the wrong question for a wallet
 * credit, which core banking never sees. It would answer "not posted" and the reconciler would close
 * a row whose money is still sitting in the wallet. DFS offers no status query, so this gate answers
 * from what the stored transaction recorded: the credit's own verdict ({@code callback.status},
 * written by {@code dfs-callback-response} when DFS accepted it) and any reversal's verdict
 * ({@code dfsReversal.status}).
 *
 * <p>Speaks the engine's gate contract — no halt = held, {@code cbc-status-txn-not-found} = not held,
 * any other halt = unknown:
 *
 * <table>
 *   <tr><th>Recorded</th><th>Answer</th></tr>
 *   <tr><td>reversal SUCCESS</td><td>not held — already backed out</td></tr>
 *   <tr><td>credit SUCCESS</td><td>held — a reversal is owed</td></tr>
 *   <tr><td>credit refused (non-zero ResultCode)</td><td>not held</td></tr>
 *   <tr><td>nothing</td><td>unknown — retries, then escalates</td></tr>
 * </table>
 *
 * <p>A reversal that went out without a readable answer is deliberately <b>not</b> recorded (the
 * {@code db-update} after it is skipped by the halt), so the gate keeps answering "held" and the next
 * attempt re-sends it under the same conversation id: DFS either applies it, or refuses it as a
 * duplicate and the row escalates. Never a second debit.
 */
@Component
public class DfsCreditStatusStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(DfsCreditStatusStage.class);

    /** The engine reads this as NOT_POSTED — same code {@code cbc-txn-status} raises. */
    static final String NOT_HELD_HALT_CODE = "cbc-status-txn-not-found";
    static final String UNKNOWN_HALT_CODE = "dfs-credit-status-unknown";

    @Override
    public String name() {
        return "dfs-credit-status";
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        Details details = request == null ? null : request.getDetails();
        String requestId = details == null ? null : details.getRequestId();
        if (details == null) {
            context.halt(name(), "DFS-S0", UNKNOWN_HALT_CODE, "No transaction details to establish the wallet state");
            return request;
        }

        if ("SUCCESS".equalsIgnoreCase(field(details, DfsCallbackResponseStage.REVERSAL_STATUS_FIELD))) {
            context.halt(name(), "DFS-S1", NOT_HELD_HALT_CODE, "DFS wallet credit already reversed");
            return request;
        }
        if ("SUCCESS".equalsIgnoreCase(field(details, DfsCallbackResponseStage.CREDIT_STATUS_FIELD))) {
            LOG.debug("[RECOVERY] requestId '{}': DFS accepted the wallet credit and no reversal is recorded", requestId);
            return request;
        }
        String creditCode = field(details, DfsCallbackResponseStage.CREDIT_RESULT_CODE_FIELD);
        if (creditCode != null && !creditCode.isBlank() && !"0".equals(creditCode.trim())) {
            context.halt(name(), "DFS-S2", NOT_HELD_HALT_CODE, "DFS refused the wallet credit (" + creditCode + ")");
            return request;
        }
        LOG.warn("[RECOVERY] requestId '{}': no DFS verdict recorded for the wallet credit — state unknown", requestId);
        context.halt(name(), "DFS-S9", UNKNOWN_HALT_CODE, "No DFS verdict recorded for the wallet credit");
        return request;
    }

    private static String field(Details details, String key) {
        return details.getAdditionalFields() == null ? null : details.getAdditionalFields().get(key);
    }
}
