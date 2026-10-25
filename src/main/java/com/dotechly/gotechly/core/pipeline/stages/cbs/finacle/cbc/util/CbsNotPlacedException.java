package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util;

/**
 * Raised when a core-banking request could <b>not be handed to jPOS at all</b> — the {@code appQBean}
 * is not registered, or the sender's country code routes to no transaction-manager queue.
 *
 * <p>The distinction this type carries is the only one that matters to an inward corridor under a
 * response deadline: nothing left this process, so the transaction is <b>definitively not posted</b>
 * and the scheme can be told FAIL outright with no reversal to follow. Contrast a request that was
 * placed and then timed out, where the posting may well have landed and the outcome is
 * <em>unconfirmed</em> — that one needs a status inquiry before anything is declared.
 *
 * <p>Stages translate this into a definitive halt code ({@code cbc-payment-failed} and friends);
 * they must never let it escape as a plain exception, because {@code PipelineExecutor} does not mark
 * the context halted for an unhandled throw, so no compensation runs and the scheme is told nothing.
 */
public class CbsNotPlacedException extends RuntimeException {

    public CbsNotPlacedException(String message) {
        super(message);
    }

    public CbsNotPlacedException(String message, Throwable cause) {
        super(message, cause);
    }
}
