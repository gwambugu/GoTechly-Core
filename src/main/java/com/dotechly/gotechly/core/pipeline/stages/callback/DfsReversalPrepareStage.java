package com.dotechly.gotechly.core.pipeline.stages.callback;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@code dfs-reversal-prepare} — turns a stored DFS wallet credit into a wallet-reversal callback.
 *
 * <p>Does for the reconciler what the RNDPS reversal handler does for a live pacs.007: sets
 * {@code requestType} so {@code callback} resolves the reversal channel and template, and supplies
 * the DFS conversation id through {@code callback.pac007reversalid} (DFS rejects a reused
 * {@code OriginatorConversationID} with AG45021, so the reversal cannot go under the credit's id).
 *
 * <p><b>The conversation id is deterministic and survives retries</b> — {@link #CONVERSATION_PREFIX}
 * plus the credit's requestId, kept on {@code dfsReversal.conversationId}. A retried reversal that
 * had in fact landed is then refused by DFS as a duplicate rather than applied again. Generating a
 * fresh id per attempt would defeat that, and is the one way this path could debit a wallet twice.
 */
@Component
public class DfsReversalPrepareStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(DfsReversalPrepareStage.class);

    static final String CONVERSATION_PREFIX = "RV";
    static final String CONVERSATION_ID_FIELD = "dfsReversal.conversationId";
    /** The override {@code CallbackStage} reads for the DFS conversation id. */
    static final String CALLBACK_CONVERSATION_FIELD = "callback.pac007reversalid";

    @Override
    public String name() {
        return "dfs-reversal-prepare";
    }

    @Override
    public int defaultOrder() {
        return 4900;   // immediately before callback (5000)
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        Details details = request == null ? null : request.getDetails();
        if (details == null || details.getRequestId() == null || details.getRequestId().isBlank()) {
            context.halt(name(), "DFS-P0", DfsCallbackResponseStage.REVERSAL_UNCONFIRMED_HALT_CODE,
                    "Cannot prepare a DFS reversal without the credit's requestId");
            return request;
        }
        String conversationId = details.getAdditionalField(CONVERSATION_ID_FIELD);
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = CONVERSATION_PREFIX + details.getRequestId().trim();
            details.addAdditionalField(CONVERSATION_ID_FIELD, conversationId);
        }
        details.addAdditionalField(CALLBACK_CONVERSATION_FIELD, conversationId);
        details.setRequestType(DfsCallbackResponseStage.DFS_REVERSAL_REQUEST_TYPE);
        LOG.info("[RECOVERY] requestId '{}': DFS wallet reversal prepared (conversationId={})",
                details.getRequestId(), conversationId);
        return request;
    }
}
