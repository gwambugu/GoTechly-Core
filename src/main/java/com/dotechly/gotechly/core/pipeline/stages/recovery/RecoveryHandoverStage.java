package com.dotechly.gotechly.core.pipeline.stages.recovery;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.recovery.DeferredRecoveryService;
import com.dotechly.gotechly.core.recovery.RecoveryProperties;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@code recovery-handover} — stops a scheme-initiated reversal from backing a credit out a second
 * time when the reconciler already owns it.
 *
 * <p>Belongs <b>first</b> in each branch that reverses on the scheme's say-so — on R-NDPS,
 * {@code rndps-reversal-onus} and {@code rndps-reversal-dfs} — before any stage moves money:
 *
 * <pre>
 *   rndps-reversal-dfs:
 *     - recovery-handover        # reconciler already reversing? answer, don't reverse again
 *     - callback                 # DFS wallet reversal
 *     - ...
 *   reversal-handled-by-recovery:
 *     - in-callback-handler      # ACSC: the reversal is honoured, the reconciler finishes it
 *     - db-update
 * </pre>
 *
 * <p>The decision is {@link DeferredRecoveryService#handOverToScheme}. When the reconciler has not
 * started, this cancels its row and lets the scheme's reversal run — the normal case, a pacs.007
 * inside the grace period. When it has, this halts {@link #HALT_CODE} so the scheme is answered
 * without a second reversal: core banking would refuse a duplicate CBS reversal anyway, but a DFS
 * wallet would not, because the pacs.007 carries a different conversation id.
 *
 * <p>Keys on {@code Details.requestId}, which in these branches is the <em>original</em> credit's id
 * (the reversal handler merges the pacs.007 into the stored row) — the same id the recovery row was
 * raised under.
 *
 * <p>A failure to consult the reconciler lets the reversal proceed: refusing a scheme reversal is
 * never an option, and the CBS path has its own duplicate guard.
 */
@Component
public class RecoveryHandoverStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(RecoveryHandoverStage.class);

    /** Raised when the reconciler has already started (or finished) backing this credit out. */
    public static final String HALT_CODE = "reversal-handled-by-recovery";
    static final String REASON_CODE = "RCV-01";

    private final DeferredRecoveryService service;
    private final RecoveryProperties properties;

    public RecoveryHandoverStage(DeferredRecoveryService service, RecoveryProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "recovery-handover";
    }

    @Override
    public int defaultOrder() {
        // Before any money-moving stage; the YAML order is what actually applies.
        return 900;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        if (!properties.isEnabled()) {
            return request;
        }
        Details details = request == null ? null : request.getDetails();
        String requestId = details == null ? null : details.getRequestId();
        try {
            if (service.handOverToScheme(requestId) == DeferredRecoveryService.SchemeReversal.HANDLED_BY_RECOVERY) {
                context.halt(name(), REASON_CODE, HALT_CODE,
                        "Reversal already in the reconciler's hands for " + requestId);
            }
        } catch (Exception e) {
            LOG.error("[RECOVERY] could not consult the reconciler for scheme reversal of '{}' — letting the "
                    + "reversal proceed: {}", requestId, e.getMessage(), e);
        }
        return request;
    }
}
