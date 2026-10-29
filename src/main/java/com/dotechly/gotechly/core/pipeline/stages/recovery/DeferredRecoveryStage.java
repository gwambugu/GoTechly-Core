package com.dotechly.gotechly.core.pipeline.stages.recovery;

import com.dotechly.gotechly.core.domain.DeferredRecovery;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.recovery.DeferredRecoveryQueue;
import com.dotechly.gotechly.core.recovery.DeferredRecoveryService;
import com.dotechly.gotechly.core.recovery.RecoveryModel;
import com.dotechly.gotechly.core.recovery.RecoveryProperties;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * {@code deferred-recovery} — hands a transaction to the reconciler.
 *
 * <p>Belongs at the <b>end of a compensation list</b>, after the stage that tells the scheme its
 * outcome. That placement is the whole trigger mechanism, and it needs no new machinery: the halt
 * code already selects the compensation, so listing this stage in
 * {@code compensation.cbc-payment-unconfirmed} is exactly "when a posting is unconfirmed, reconcile
 * it later".
 *
 * <pre>
 *   compensation:
 *     cbc-payment-unconfirmed:
 *       - in-callback-handler     # tell R-NDPS, inside the 40s
 *       - db-update
 *       - deferred-recovery       # then make core banking match what we just said
 * </pre>
 *
 * <p>Put it after the callback, never before: the callback is on a deadline and enqueuing is not.
 *
 * <p><b>It never halts and never fails the pipeline.</b> A compensation that has already told the
 * scheme its answer must finish; failing here would strand a transaction in the one state this whole
 * module exists to drain.
 */
@Component
public class DeferredRecoveryStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(DeferredRecoveryStage.class);

    /** Corridors that know what they told the scheme stamp it here. */
    public static final String DECLARED_OUTCOME_FIELD = "declaredOutcome";

    /**
     * Optional. Selects {@code {phase}-{profile}} recovery phases where the corridor configured them
     * — e.g. {@code dfs-wallet} for a credit held in a DFS wallet rather than core banking. See
     * {@code DeferredRecoveryEngine.phaseFor}.
     */
    public static final String RECOVERY_PROFILE_FIELD = "recoveryProfile";

    /** Statuses that mean "we reported this as settled". */
    private static final Set<String> SUCCESS_STATUSES = Set.of("COMPLETED", "SUCCESS", "POSTED", "ACSC", "ACCC");
    /** Statuses that mean "we reported this as refused". */
    private static final Set<String> FAILED_STATUSES = Set.of("FAILED", "REJECTED", "RJCT", "DECLINED");
    /**
     * Statuses that mean "we told them we did not know yet". Distinct from an unreadable status: this
     * is a corridor that reported {@code PDNG} on purpose and will be asked again, so the reconciler
     * resolves core banking rather than repairing it.
     */
    private static final Set<String> PENDING_STATUSES = Set.of("PENDING", "PDNG", "ACSP", "IN_PROGRESS");

    private final DeferredRecoveryService service;
    private final DeferredRecoveryQueue queue;
    private final RecoveryProperties properties;

    public DeferredRecoveryStage(DeferredRecoveryService service, DeferredRecoveryQueue queue,
                                 RecoveryProperties properties) {
        this.service = service;
        this.queue = queue;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "deferred-recovery";
    }

    @Override
    public int defaultOrder() {
        // After callback (5000) and the notification stages: enqueuing must not delay the answer the
        // scheme is waiting on.
        return 6000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        if (!properties.isEnabled()) {
            LOG.debug("[RECOVERY] stage invoked but gotechly.recovery.enabled=false — not tracking");
            return request;
        }
        Details details = request.getDetails();
        String requestId = details == null ? null : details.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            LOG.error("[RECOVERY] cannot track a transaction with no requestId (paymentType='{}', phase='{}') — "
                            + "this transaction will not be reconciled",
                    context.getPaymentType(), context.getPipelinePhase());
            return request;
        }

        try {
            RecoveryModel.DeclaredOutcome declared = declaredOutcome(details, requestId);
            String triggerHaltCode = firstNonBlank(
                    context.getAttribute("original.haltCode"), context.getHaltCode());

            DeferredRecovery row = service.enqueue(requestId, context.getPaymentType(),
                    context.getDirection(), declared, triggerHaltCode);

            // Read the outcome back off the row rather than trusting the one just computed: enqueue is
            // idempotent, so an existing row keeps whatever it was first recorded with, and escalating
            // on the freshly-computed value would condemn a row that already knows its answer.
            if (row.getDeclaredOutcome() == RecoveryModel.DeclaredOutcome.UNKNOWN) {
                service.escalate(row.getId(),
                        "Cannot establish what the scheme was told (trigger '" + triggerHaltCode
                                + "', status '" + details.getStatus() + "'). Both repairs move money, "
                                + "so this needs a human. A scheme reversal arriving later closes it.");
                LOG.error("[RECOVERY] requestId '{}' recorded as UNKNOWN and escalated — the corridor "
                                + "did not stamp {} and status '{}' says nothing about what R-NDPS was "
                                + "told. No repair will run.",
                        requestId, DECLARED_OUTCOME_FIELD, details.getStatus());
                return request;
            }

            // The row is what guarantees the work happens. The queue is a fast path; a refusal here
            // only means the sweeper gets there first.
            boolean queued = queue.offer(row);
            LOG.info("[RECOVERY] tracking requestId '{}' (declared={}, trigger='{}', queuedNow={})",
                    requestId, row.getDeclaredOutcome(), triggerHaltCode, queued);
        } catch (Exception e) {
            // Losing the enqueue is bad, but failing the compensation is worse — the scheme has
            // already been answered and the remaining stages still have to run.
            LOG.error("[RECOVERY] FAILED to track requestId '{}' — this transaction will NOT be reconciled "
                    + "automatically and must be checked by hand: {}", requestId, e.getMessage(), e);
        }
        return request;
    }

    /**
     * What we told the scheme.
     *
     * <p>Prefers an explicit stamp from the corridor, because only the corridor truly knows what its
     * callback said, then falls back to the transaction status.
     *
     * <p>When neither answers, the outcome is {@link RecoveryModel.DeclaredOutcome#UNKNOWN} unless an
     * operator has configured {@code gotechly.recovery.default-declared-outcome} for this deployment.
     * <b>It does not guess.</b> This one value decides which repair runs, and the two repairs move
     * money in opposite directions — reversing a credit reported as settled, or posting one reported
     * as rejected — so there is no direction that is safe to assume. UNKNOWN is escalated instead.
     */
    private RecoveryModel.DeclaredOutcome declaredOutcome(Details details, String requestId) {
        Map<String, String> additionalFields = details.getAdditionalFields();
        if (additionalFields != null) {
            String explicit = additionalFields.get(DECLARED_OUTCOME_FIELD);
            if (explicit != null && !explicit.isBlank()) {
                try {
                    return RecoveryModel.DeclaredOutcome.valueOf(explicit.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    LOG.warn("[RECOVERY] requestId '{}' carries an unrecognised {}='{}' — falling back to status",
                            requestId, DECLARED_OUTCOME_FIELD, explicit);
                }
            }
        }
        String status = details.getStatus() == null ? "" : details.getStatus().trim().toUpperCase(Locale.ROOT);
        if (SUCCESS_STATUSES.contains(status)) {
            return RecoveryModel.DeclaredOutcome.SUCCESS;
        }
        if (FAILED_STATUSES.contains(status)) {
            return RecoveryModel.DeclaredOutcome.FAILED;
        }
        if (PENDING_STATUSES.contains(status)) {
            return RecoveryModel.DeclaredOutcome.PENDING;
        }
        RecoveryModel.DeclaredOutcome configured = properties.getDefaultDeclaredOutcome();
        if (configured != null) {
            LOG.warn("[RECOVERY] requestId '{}' has status '{}', which says nothing about what the scheme "
                            + "was told — using the configured default {}. Stamp "
                            + "Details.additionalFields.{} in the corridor to stop relying on it.",
                    requestId, details.getStatus(), configured, DECLARED_OUTCOME_FIELD);
            return configured;
        }
        return RecoveryModel.DeclaredOutcome.UNKNOWN;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return (b != null && !b.isBlank()) ? b : null;
    }
}
