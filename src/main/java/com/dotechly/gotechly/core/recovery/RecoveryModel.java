package com.dotechly.gotechly.core.recovery;

/**
 * The vocabulary the reconciler is built on. Three small enumerations and one decision table; the
 * rest of the module is plumbing around them.
 *
 * <p>The idea that makes this tractable is keeping two facts apart:
 * <ul>
 *   <li>{@link DeclaredOutcome} — <b>what we told the scheme</b>. Decided once, inside the response
 *       deadline, and never revised. A pacs.002 that has left the building cannot be taken back.</li>
 *   <li>{@link CbsState} — <b>what core banking actually holds</b>. Discovered by the gate, and
 *       potentially different every time we ask.</li>
 * </ul>
 *
 * <p>The reconciler's whole contract is then one sentence: <i>make {@code CbsState} agree with
 * {@code DeclaredOutcome}, or escalate.</i> It never decides an outcome, so it can never contradict
 * the answer the scheme already has.
 */
public final class RecoveryModel {

    private RecoveryModel() {
    }

    /** What we told the scheme. Fixed at enqueue time. */
    public enum DeclaredOutcome {
        /** We reported the transfer as accepted/settled. Core banking must end up holding it. */
        SUCCESS,
        /** We reported the transfer as rejected. Core banking must end up NOT holding it. */
        FAILED,
        /**
         * We reported that we did not yet know — R-NDPS {@code PDNG}, and the scheme will come back
         * with a pacs.028 status enquiry.
         *
         * <p>Nothing has been asserted, so there is nothing to repair: core banking is already
         * consistent with "unknown" whichever way it lands. What the reconciler owes this transaction
         * is an <b>answer</b>, not a correction — keep asking the gate until core banking is
         * definitive, record it, and stop. The stored state is then what the status enquiry is
         * answered from.
         *
         * <p>Repairing a PENDING transaction would be the worst error available here: reversing a
         * posting we never told anyone had failed, or reposting one we never said had settled.
         */
        PENDING,

        /**
         * <b>We could not establish what the scheme was told.</b> The corridor stamped nothing and the
         * transaction status says nothing either.
         *
         * <p>This is not a fourth outcome to reconcile against — it is the absence of one, and it is
         * recorded rather than guessed because both repairs move money in opposite directions. A row
         * in this state goes straight to {@link RecoveryState#NEEDS_ATTENTION}: it is tracked, it is
         * counted on the metrics endpoint, and a human decides. Picking a direction here would turn a
         * missing fact into a debit or a credit.
         */
        UNKNOWN
    }

    /**
     * What core banking holds, as reported by the gate stage. Mirrors the three answers
     * {@code cbc-txn-status} can give — and the third one is why this module exists.
     */
    public enum CbsState {
        /** The gate saw field 127 flag {@code Y}. Definitive. */
        POSTED,
        /** The gate saw field 127 flag {@code N}. Definitive. */
        NOT_POSTED,
        /** No answer, or an answer that settles nothing. Ask again later; conclude nothing. */
        UNKNOWN
    }

    /** Lifecycle of a recovery row. Only the first two are live; the rest are terminal. */
    public enum RecoveryState {
        /** Waiting for its next attempt. */
        PENDING,
        /** Claimed by an instance and being worked right now. */
        IN_PROGRESS,
        /** Core banking agrees with what the scheme was told. Nothing left to do. */
        RECONCILED,
        /**
         * Out of attempts or past the deadline, and still inconsistent. <b>Not a failure to ignore:</b>
         * the scheme holds an answer that core banking does not match, and only a human can close
         * that. Alarm on this.
         */
        NEEDS_ATTENTION,
        /** An operator called it off. */
        CANCELLED
    }

    /** What the engine should do next, given the declared outcome and what the gate found. */
    public enum Action {
        /** Already consistent — settle the row. */
        NOTHING_TO_DO,
        /** Declared FAILED but core banking holds the posting: back it out. */
        REVERSE,
        /** Declared SUCCESS but core banking has no posting: put it on. */
        REPOST,
        /** The gate settled nothing. Try again after the backoff. */
        RETRY
    }

    /**
     * The decision table, in full. Six cells, and the two {@code RETRY} cells are the ones that make
     * the loop necessary at all.
     *
     * <pre>
     *   declared   cbs state     action
     *   --------   -----------   ---------------
     *   FAILED     NOT_POSTED    NOTHING_TO_DO    already consistent
     *   FAILED     POSTED        REVERSE          back out a posting the scheme thinks was rejected
     *   FAILED     UNKNOWN       RETRY
     *   SUCCESS    POSTED        NOTHING_TO_DO    already consistent
     *   SUCCESS    NOT_POSTED    REPOST           put on a posting the scheme thinks settled
     *   SUCCESS    UNKNOWN       RETRY
     *   PENDING    NOT_POSTED    NOTHING_TO_DO    resolved; the enquiry can now be answered
     *   PENDING    POSTED        NOTHING_TO_DO    resolved; the enquiry can now be answered
     *   PENDING    UNKNOWN       RETRY
     *   UNKNOWN    (any)         RETRY            never reached — escalated before it gets here
     * </pre>
     *
     * <p>The PENDING row never repairs. We asserted nothing, so there is nothing to correct — the
     * loop exists only to turn UNKNOWN into a definite state the scheme's status enquiry can be
     * answered from.
     *
     * <p>Note that {@code NOT_POSTED} is only ever reached from the gate's definitive {@code N}, never
     * inferred from silence — the whole table is worthless if {@code UNKNOWN} can leak into it.
     */
    public static Action decide(DeclaredOutcome declared, CbsState cbs) {
        if (declared == null || cbs == null || cbs == CbsState.UNKNOWN) {
            return Action.RETRY;
        }
        return switch (declared) {
            case FAILED -> cbs == CbsState.POSTED ? Action.REVERSE : Action.NOTHING_TO_DO;
            case SUCCESS -> cbs == CbsState.POSTED ? Action.NOTHING_TO_DO : Action.REPOST;
            case PENDING -> Action.NOTHING_TO_DO;
            // Defensive only: the stage escalates an UNKNOWN row at enqueue and the engine refuses it
            // again before consulting this table. RETRY is the answer that cannot move money, which
            // is the only safe thing to say about a transaction whose declared outcome is missing.
            case UNKNOWN -> Action.RETRY;
        };
    }
}
