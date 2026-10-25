package com.dotechly.gotechly.core.pipeline.audit;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;

import java.util.List;

/**
 * Observer interface for pipeline execution lifecycle events.
 * Implementations are Spring beans discovered via DI and invoked by
 * {@link com.dotechly.gotechly.core.pipeline.PipelineExecutor}
 * after a pipeline completes — regardless of success, halt, or error.
 *
 * <p>This decouples cross-cutting concerns (audit, metrics, alerting) from
 * the pipeline stage execution loop. Unlike a pipeline stage, listeners
 * <strong>always</strong> run — even when a stage halts or throws.</p>
 */
public interface PipelineExecutionListener {

    /**
     * Called once after the pipeline finishes executing all stages (or is halted/errored).
     * Receives the complete execution trail with before/after snapshots for each stage.
     *
     * @param context     the pipeline context with metadata (paymentType, direction, phase, halt info)
     * @param finalResult the final PaymentLogSPI state after all executed stages
     * @param trail       ordered list of per-stage execution records with JSON snapshots
     */
    void onPipelineComplete(PipelineContext context, PaymentLogSPI finalResult, List<StageExecutionRecord> trail);
}
