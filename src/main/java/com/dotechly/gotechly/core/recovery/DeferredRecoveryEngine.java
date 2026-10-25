package com.dotechly.gotechly.core.recovery;

import com.dotechly.gotechly.core.domain.DeferredRecovery;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineExecutor;
import com.dotechly.gotechly.core.pipeline.PipelineRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.PaymentLogLookup;
import com.dotechly.gotechly.spi.service.dto.payment.PSP;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Runs one reconciliation attempt: gate, decide, repair.
 *
 * <p>Every step goes through the ordinary pipeline machinery — {@link PipelineRegistry} resolves the
 * stages for {@code (direction, paymentType, phase)} and {@link PipelineExecutor} runs them — so a
 * corridor configures its recovery in the same plugin config, with the same stage names and the same
 * compensation semantics as everything else. There is no second, parallel way to talk to core
 * banking here, which is the point: the gate really is {@code cbc-txn-status} and the repair really
 * is {@code cbc-txn-reversal}.
 *
 * <p>Runs on worker threads, never on a Netty event loop, so the blocking
 * {@link PipelineExecutor#execute} is the right entry point.
 */
@Component
public class DeferredRecoveryEngine {

    private static final Logger LOG = LoggerFactory.getLogger(DeferredRecoveryEngine.class);

    /** {@code PSP.status} the callback handler records once the scheme acknowledged the message. */
    private static final String SCHEME_ACKNOWLEDGED = "POSTED";

    /**
     * Resolved on use, not on construction, and that is <b>required rather than stylistic</b>.
     *
     * <p>{@link PipelineRegistry} takes {@code List<PipelineStage>} in its constructor, and
     * {@code deferred-recovery} is a stage. Injecting the registry (or {@link PipelineExecutor},
     * which itself takes the registry) directly closes a bean cycle Spring refuses to start:
     *
     * <pre>
     *   PipelineRegistry → List&lt;PipelineStage&gt; → DeferredRecoveryStage
     *                    → DeferredRecoveryQueue → DeferredRecoveryEngine → PipelineRegistry
     * </pre>
     *
     * <p>The cycle is only in <em>construction</em>: at runtime the dependency is strictly one-way,
     * because nothing is reconciled until long after the context is up. Deferring the lookup states
     * that, and does it without a proxy — {@code @Lazy} would work too, but would wrap two beans on
     * a path that has no other reason to be proxied.
     *
     * <p>Do not "simplify" these back to plain constructor injection. The application fails to start,
     * and the failure surfaces as an unrelated-looking {@code pluginLoader} error at the top of the
     * chain.
     */
    private final ObjectFactory<PipelineRegistry> registryProvider;
    private final ObjectFactory<PipelineExecutor> executorProvider;

    private final PaymentLogLookup paymentLogLookup;
    private final DeferredRecoveryService service;
    private final RecoveryProperties properties;

    public DeferredRecoveryEngine(ObjectFactory<PipelineRegistry> registryProvider,
                                  ObjectFactory<PipelineExecutor> executorProvider,
                                  PaymentLogLookup paymentLogLookup, DeferredRecoveryService service,
                                  RecoveryProperties properties) {
        this.registryProvider = registryProvider;
        this.executorProvider = executorProvider;
        this.paymentLogLookup = paymentLogLookup;
        this.service = service;
        this.properties = properties;
    }

    /**
     * One attempt against one row. Always reaches a durable transition — reschedule, reconcile or
     * escalate — because a row that is left {@code IN_PROGRESS} with nobody working it is only
     * recovered by the lease expiring, and that is a slow way to notice a bug.
     */
    public void attempt(DeferredRecovery row) {
        Long id = row.getId();
        String requestId = row.getRequestId();
        if (row.getDeclaredOutcome() == RecoveryModel.DeclaredOutcome.UNKNOWN) {
            // The stage escalates these at enqueue, so reaching a worker means an operator requeued
            // one. Requeuing does not supply the missing fact, and every repair from here would be a
            // coin toss with money on it, so it goes straight back to the queue a human works.
            service.escalate(id, "Declared outcome is still UNKNOWN — what the scheme was told has to be "
                    + "established before this transaction can be reconciled.");
            return;
        }
        try {
            Optional<PaymentLogSPI> loaded = paymentLogLookup.findByOriginalRequestId(requestId);
            if (loaded.isEmpty()) {
                // Nothing to reconcile against, and no amount of retrying will conjure the row.
                service.escalate(id, "No payment_log row for requestId '" + requestId + "'");
                return;
            }
            PaymentLogSPI log = loaded.orElseThrow();

            GateResult gate = runGate(row, log);
            RecoveryModel.Action action = suppressRepostIfSchemeRefused(
                    row, log, RecoveryModel.decide(row.getDeclaredOutcome(), gate.state()));

            LOG.info("[RECOVERY] requestId '{}' attempt {}: declared={} cbs={} → {}",
                    requestId, row.getAttempts() + 1, row.getDeclaredOutcome(), gate.state(), action);

            switch (action) {
                case RETRY -> service.reschedule(id, gate.state(), action,
                        properties.getPhases().getGate(), gate.detail());
                case NOTHING_TO_DO -> {
                    // Already consistent. The confirm phase is bookkeeping only; a failure in it must
                    // not reopen a transaction that core banking and the scheme already agree on.
                    String detail = runConfirm(row, gate.log());
                    service.reconciled(id, gate.state(), action, properties.getPhases().getConfirm(), detail);
                }
                case REVERSE -> repair(row, gate, properties.getPhases().getReverse(), action);
                case REPOST -> repair(row, gate, properties.getPhases().getRepost(), action);
            }
        } catch (Exception e) {
            // A bug here must not strand the row. Reschedule and let the attempt budget bound it.
            LOG.error("[RECOVERY] attempt failed for requestId '{}': {}", requestId, e.getMessage(), e);
            service.reschedule(id, row.getLastCbsState(), RecoveryModel.Action.RETRY, null,
                    "Recovery attempt error: " + e);
        }
    }

    /**
     * Cancels a repost when the scheme refused the callback that declared the payment settled.
     *
     * <p>The declared outcome is stamped <em>before</em> the pacs.002 goes out, so a transaction can
     * carry {@code SUCCESS} and still have had that message rejected. R-NDPS answers {@code NOT_ACK}
     * when the callback misses its deadline: it has cancelled the transfer on its side and will not
     * settle it. Reposting then puts a credit on the books that nobody will pay us for.
     *
     * <p><b>Keyed on {@code PSP.status}, not on the response text.</b> {@code recordPsp} sets the
     * status on every path, so it is immune to how the counterparty words its refusal — the body has
     * been seen both as a bare {@code NOT_ACK} and, per the callback handler's own javadoc, as
     * {@code NOT_ACK: This callback is late and the transaction <id> is cancelled!}. Matching the
     * text with an equality test passes the first and silently misses the second.
     *
     * <p>Status also widens the guard to the case that matters most in practice: a callback that was
     * never delivered at all. A core-banking timeout and a failed callback tend to arrive in the same
     * outage, and a scheme that heard nothing behaves exactly as it does after a {@code NOT_ACK}.
     *
     * <p><b>Only {@code REPOST} is cancelled.</b> Leaving {@code REVERSE} alone is deliberate: R-NDPS
     * follows a {@code NOT_ACK} with its own pacs.007, which backs the entry out through
     * {@code credit-reversal}. Reversing here as well would put two reversals on one posting.
     *
     * <p>Absence of evidence is not evidence of refusal — a transaction with no PSP block, or none
     * with a status, is left to repost. On this corridor both errors cost money (an unpaid
     * beneficiary we have already reported as paid, or a credit that will never be settled), so the
     * guard fires on a recorded refusal and on nothing weaker.
     */
    private RecoveryModel.Action suppressRepostIfSchemeRefused(DeferredRecovery row, PaymentLogSPI log,
                                                               RecoveryModel.Action action) {
        if (action != RecoveryModel.Action.REPOST) {
            return action;
        }
        PSP psp = log.getPsp();
        String pspStatus = psp == null ? null : psp.getStatus();
        if (pspStatus == null || pspStatus.isBlank()) {
            LOG.warn("[RECOVERY] requestId '{}': reposting without a recorded scheme acknowledgement "
                            + "(no PSP status). If this corridor answers the scheme, check that "
                            + "in-callback-handler runs before deferred-recovery.",
                    row.getRequestId());
            return action;
        }
        if (SCHEME_ACKNOWLEDGED.equalsIgnoreCase(pspStatus.trim())) {
            return action;
        }
        LOG.warn("[RECOVERY] requestId '{}': the scheme did not accept our callback (PSP status='{}', "
                        + "'{}') — NOT reposting a credit it will not settle. Expect the scheme's own "
                        + "reversal if core banking did post.",
                row.getRequestId(), pspStatus, psp.getResponseDesc());
        return RecoveryModel.Action.NOTHING_TO_DO;
    }

    /**
     * Asks core banking what it holds.
     *
     * <p>The gate's verdict comes from the <b>halt code</b>, not from a status string: that is the one
     * signal {@code cbc-txn-status} is contractually obliged to set, and it already encodes the
     * three-way answer this module needs. A gate phase that resolves to no stages is a
     * misconfiguration and yields {@code UNKNOWN} rather than a cheerful default — guessing here
     * would drive a reversal off no evidence at all.
     */
    private GateResult runGate(DeferredRecovery row, PaymentLogSPI log) {
        String phase = properties.getPhases().getGate();
        List<PipelineStage> stages = resolvePhase(row, phase);
        if (stages.isEmpty()) {
            LOG.warn("[RECOVERY] no stages configured for gate phase '{}' ({} / {}) — cannot establish "
                            + "core-banking state, treating as UNKNOWN",
                    phase, row.getDirection(), row.getPaymentType());
            return new GateResult(RecoveryModel.CbsState.UNKNOWN, log, "Gate phase '" + phase + "' has no stages");
        }

        PipelineContext context = new PipelineContext(row.getPaymentType(), row.getDirection(), phase);
        PaymentLogSPI result = executorProvider.getObject().execute(stages, context, log);

        if (!context.isHalted()) {
            return new GateResult(RecoveryModel.CbsState.POSTED, result, "Gate completed without halting");
        }
        String haltCode = context.getHaltCode();
        String detail = context.getHaltReasonCode() + " / " + context.getHaltReason();
        if (TransactionStatusHaltCodes.NOT_FOUND.equals(haltCode)) {
            return new GateResult(RecoveryModel.CbsState.NOT_POSTED, result, detail);
        }
        // Includes cbc-status-txn-unconfirmed and anything unrecognised. Erring toward "still in
        // flight" is deliberate: an unclassified halt must never authorise a reversal.
        return new GateResult(RecoveryModel.CbsState.UNKNOWN, result, detail);
    }

    /**
     * Runs a repair phase, and decides from the halt code whether the failure is worth retrying.
     *
     * <p>{@code cbc-txn-reversal} and {@code cbc-txn-payment} complete silently only on success, so
     * any halt means the repair did not take. What it does <b>not</b> tell you on its own is whether
     * trying again could help — or whether it needed doing at all — and the three cases want
     * completely different handling:
     *
     * <ul>
     *   <li><b>Core banking did not answer</b> ({@code cbc-payment-unconfirmed}) — the posting may
     *       even have landed. Reschedule; the next attempt's gate settles it. This is the case the
     *       backoff and the attempt budget exist for.</li>
     *   <li><b>Core banking refused</b> ({@code cbc-payment-failed}, i.e. a DE39 the host declined on)
     *       — a customer under a credit restriction declines identically on every repost. Retrying
     *       spends twelve attempts over six hours to arrive at the same {@code NEEDS_ATTENTION} the
     *       first answer already justified, and puts eleven pointless postings through core banking
     *       on the way. Escalate.</li>
     *   <li><b>Core banking already holds the result</b> ({@code cbc-reversal-already-done}) — a
     *       duplicate pacs.007 got there first, or an earlier attempt landed and lost its answer.
     *       The repair is unnecessary rather than failed, so the row is <em>reconciled</em>. Checked
     *       first, because both other readings would raise a false alarm on a correct row.</li>
     * </ul>
     *
     * <p>Which halt codes mean which is {@link RecoveryProperties#isAlreadySatisfiedHaltCode} and
     * {@link RecoveryProperties#isTerminalHaltCode} — config, not constants here, because a
     * corridor's repair stages come from its plugin config and need not be the CBS ones at all.
     *
     * <p><b>This does not decide anything about the money.</b> An escalated repost still leaves the
     * scheme holding "settled" against a core banking that refused to post: it is a real open
     * position, and escalating is how a human is told about it sooner rather than six hours later.
     */
    private void repair(DeferredRecovery row, GateResult gate, String phase, RecoveryModel.Action action) {
        List<PipelineStage> stages = resolvePhase(row, phase);
        if (stages.isEmpty()) {
            service.escalate(row.getId(), "No stages configured for repair phase '" + phase
                    + "' (" + row.getDirection() + " / " + row.getPaymentType() + "); declared="
                    + row.getDeclaredOutcome() + " cbs=" + gate.state());
            return;
        }

        PipelineContext context = new PipelineContext(row.getPaymentType(), row.getDirection(), phase);
        executorProvider.getObject().execute(stages, context, gate.log());

        if (context.isHalted()) {
            String haltCode = context.getHaltCode();
            // Checked before the terminal list: this halt says core banking already holds the end
            // state the repair was after, which is the outcome, not a failure to reach it. Reading it
            // as a refusal would call a human to a row that is reconciled.
            if (properties.isAlreadySatisfiedHaltCode(haltCode)) {
                LOG.info("[RECOVERY] requestId '{}': core banking already holds the result of the {} "
                                + "({} / '{}') — reconciled without repeating it",
                        row.getRequestId(), action, context.getHaltReasonCode(), context.getHaltReason());
                service.reconciled(row.getId(), gate.state(), action, phase,
                        "Already satisfied: " + haltCode + " / " + context.getHaltReason());
                return;
            }
            if (properties.isTerminalHaltCode(haltCode)) {
                LOG.error("[RECOVERY] requestId '{}': core banking refused the {} with {} / '{}' — "
                                + "escalating instead of retrying, because that answer will not change. "
                                + "declared={} cbs={}",
                        row.getRequestId(), action, context.getHaltReasonCode(),
                        context.getHaltReason(), row.getDeclaredOutcome(), gate.state());
                service.escalate(row.getId(), gate.state(), action, phase,
                        "Core banking refused the " + action + " in phase '" + phase + "': "
                                + haltCode + " / " + context.getHaltReasonCode() + " / "
                                + context.getHaltReason() + ". Retrying cannot change a decision the "
                                + "host has already made.");
                return;
            }
            service.reschedule(row.getId(), gate.state(), action, phase,
                    "Repair halted: " + haltCode + " / " + context.getHaltReason());
            return;
        }
        service.reconciled(row.getId(), gate.state(), action, phase, "Repair completed");
    }

    /**
     * Bookkeeping for a row that needed no repair. Never fails the reconciliation: the transaction is
     * already consistent, and a notification that did not send is not a reason to keep asking core
     * banking about a settled posting.
     */
    private String runConfirm(DeferredRecovery row, PaymentLogSPI log) {
        String phase = properties.getPhases().getConfirm();
        List<PipelineStage> stages = resolvePhase(row, phase);
        if (stages.isEmpty()) {
            return "Already consistent";
        }
        try {
            PipelineContext context = new PipelineContext(row.getPaymentType(), row.getDirection(), phase);
            executorProvider.getObject().execute(stages, context, log);
            return context.isHalted()
                    ? "Already consistent; confirm phase halted: " + context.getHaltCode()
                    : "Already consistent; confirm phase ran";
        } catch (Exception e) {
            LOG.warn("[RECOVERY] confirm phase failed for requestId '{}' (ignored): {}",
                    row.getRequestId(), e.getMessage());
            return "Already consistent; confirm phase errored: " + e.getMessage();
        }
    }

    /**
     * Stages for a recovery phase, or an empty list when the corridor has not configured one.
     *
     * <p>{@link PipelineRegistry#getPipeline} answers an unconfigured {@code (direction, paymentType,
     * apiType)} by falling through to {@code gotechly.pipeline.default-stages} and, finding nothing there
     * either, <b>throwing</b> {@link IllegalStateException}. That is right for a request phase — a
     * payment arriving with nowhere to go is a deployment error and should be loud — but the recovery
     * phases are deliberately optional: an inward-only corridor has no repost path, and
     * {@code recovery-repost: stages: []} is a legitimate way to say "this must never happen
     * automatically".
     *
     * <p>Translating the throw into an empty list here keeps that distinction where the callers can
     * act on it: a missing gate is UNKNOWN and retries, a missing repair escalates to a human. Left
     * to propagate, both would land in the catch-all in {@link #attempt} and be rescheduled — so a
     * corridor that had simply forgotten to configure a phase would retry a dozen times before
     * anyone found out, with the real reason buried in a stack trace.
     */
    private List<PipelineStage> resolvePhase(DeferredRecovery row, String phase) {
        try {
            List<PipelineStage> stages = registryProvider.getObject().getPipeline(row.getDirection(), row.getPaymentType(), phase);
            return stages == null ? List.of() : stages;
        } catch (IllegalStateException e) {
            LOG.debug("[RECOVERY] no pipeline configured for phase '{}' ({} / {}): {}",
                    phase, row.getDirection(), row.getPaymentType(), e.getMessage());
            return List.of();
        }
    }

    /** What the gate established, plus the log it enriched on the way. */
    private record GateResult(RecoveryModel.CbsState state, PaymentLogSPI log, String detail) {
    }

    /** Halt codes {@code cbc-txn-status} raises, copied here so the engine does not import the stage. */
    static final class TransactionStatusHaltCodes {
        static final String NOT_FOUND = "cbc-status-txn-not-found";

        private TransactionStatusHaltCodes() {
        }
    }
}
