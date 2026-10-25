package com.dotechly.gotechly.core.pipeline.audit;

import java.time.Duration;
import java.time.Instant;

/**
 * Immutable record capturing what happened during a single pipeline stage execution.
 * Collected by the {@link com.dotechly.gotechly.core.pipeline.PipelineExecutor}
 * and passed to {@link PipelineExecutionListener} implementations after pipeline completion.
 *
 * @param stageName       the name of the stage that executed
 * @param startedAt       when the stage started
 * @param elapsed         how long the stage took
 * @param beforeSnapshot  JSON snapshot of PaymentLogSPI before the stage ran
 * @param afterSnapshot   JSON snapshot of PaymentLogSPI after the stage ran
 * @param error           exception thrown by the stage, or null if clean
 * @param halted          whether the pipeline was halted at this point
 * @param haltReason      reason for halt, or null
 * @param haltReasonCode  technical error code for halt, or null
 * @param haltCode        semantic halt code for compensation routing, or null
 * @param skipped         the stage never ran because an earlier stage halted the pipeline
 */
public record StageExecutionRecord(
        String stageName,
        Instant startedAt,
        Duration elapsed,
        String beforeSnapshot,
        String afterSnapshot,
        Throwable error,
        boolean halted,
        String haltReason,
        String haltReasonCode,
        String haltCode,
        boolean skipped
) {

    /** A stage that actually ran. Keeps the pre-skip call sites compiling unchanged. */
    public StageExecutionRecord(String stageName, Instant startedAt, Duration elapsed,
                                String beforeSnapshot, String afterSnapshot, Throwable error,
                                boolean halted, String haltReason, String haltReasonCode,
                                String haltCode) {
        this(stageName, startedAt, elapsed, beforeSnapshot, afterSnapshot, error,
                halted, haltReason, haltReasonCode, haltCode, false);
    }

    /**
     * A stage the executor walked past because the pipeline was already halted.
     *
     * <p>Recorded so the audit trail can distinguish "skipped by the halt" from "never configured"
     * — without it a six-stage compensation that halts on stage two leaves four stages with no
     * trace at all, and the trail reads as though they were never in the pipeline.
     *
     * <p>Carries the state at the moment of the skip as both snapshots, so the diff is empty and
     * the record adds nothing to the summary's changed-field count.
     */
    public static StageExecutionRecord skipped(String stageName, Instant at, String snapshot,
                                               String haltReason, String haltReasonCode, String haltCode) {
        return new StageExecutionRecord(stageName, at, Duration.ZERO, snapshot, snapshot,
                null, true, haltReason, haltReasonCode, haltCode, true);
    }
}
