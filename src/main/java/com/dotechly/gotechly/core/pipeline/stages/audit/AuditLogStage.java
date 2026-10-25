package com.dotechly.gotechly.core.pipeline.stages.audit;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Legacy audit logging stage — retained for backward compatibility.
 *
 * <p>Full audit trail persistence (per-stage diffs, error capture, DB writes) is now handled by
 * {@link com.dotechly.gotechly.core.pipeline.audit.AuditPipelineListener},
 * which runs as an executor-level observer and is <strong>always</strong> invoked —
 * even when a stage halts or throws an exception.</p>
 *
 * <p>This stage is no longer included in default pipeline configurations.
 * If added explicitly, it acts as a lightweight TRACE-level logger.</p>
 */
@Component
public class AuditLogStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogStage.class);

    @Override
    public String name() {
        return "audit-log";
    }

    @Override
    public int defaultOrder() {
        return 2000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        LOG.trace("[AUDIT-STAGE] paymentType='{}', phase='{}', requestId='{}', status='{}', halted={}",
                context.getPaymentType(),
                context.getPipelinePhase(),
                request.getDetails() != null ? request.getDetails().getRequestId() : "unknown",
                request.getDetails() != null ? request.getDetails().getStatus() : "unknown",
                context.isHalted());

        return request;
    }
}
