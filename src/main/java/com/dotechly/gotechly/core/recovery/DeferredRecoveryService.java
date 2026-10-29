package com.dotechly.gotechly.core.recovery;

import com.dotechly.gotechly.core.domain.DeferredRecovery;
import com.dotechly.gotechly.core.repository.DeferredRecoveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every durable state transition of a recovery row, in one place.
 *
 * <p>The engine never touches the repository directly: it is a state machine, and having the
 * transitions in one class is what makes "can this row be lost, worked twice, or silently dropped?"
 * a question you can answer by reading a hundred lines.
 */
@Service
public class DeferredRecoveryService {

    private static final Logger LOG = LoggerFactory.getLogger(DeferredRecoveryService.class);

    private final DeferredRecoveryRepository repository;
    private final RecoveryProperties properties;

    public DeferredRecoveryService(DeferredRecoveryRepository repository, RecoveryProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Records a transaction for reconciliation, or returns the existing row.
     *
     * <p>Idempotent on {@code requestId}: a corridor that halts, compensates and halts again inside
     * the compensation would otherwise enqueue the same transaction twice, and two rows means two
     * instances reversing one posting. The unique constraint is the real guard — the read-then-write
     * below races, so the constraint violation is caught and resolved by re-reading rather than
     * propagated.
     */
    @Transactional
    public DeferredRecovery enqueue(String requestId, String paymentType, String direction,
                                    RecoveryModel.DeclaredOutcome declaredOutcome, String triggerHaltCode) {
        Optional<DeferredRecovery> existing = repository.findByRequestId(requestId);
        if (existing.isPresent()) {
            DeferredRecovery row = existing.orElseThrow();
            LOG.info("[RECOVERY] requestId '{}' is already tracked (state={}, attempts={}) — not re-enqueued",
                    requestId, row.getState(), row.getAttempts());
            return row;
        }
        Instant now = Instant.now();
        DeferredRecovery row = new DeferredRecovery();
        row.setRequestId(requestId);
        row.setPaymentType(paymentType);
        row.setDirection(direction);
        row.setDeclaredOutcome(declaredOutcome);
        row.setTriggerHaltCode(triggerHaltCode);
        row.setState(RecoveryModel.RecoveryState.PENDING);
        row.setAttempts(0);
        // First attempt is immediate unless the trigger carries a grace period: core banking may
        // already have recovered by the time the compensation finished, and the cheapest
        // reconciliation is the one that happens at once. A trigger where the scheme may still do the
        // repair itself (rndps-callback-cancelled → its own pacs.007) waits that out first.
        row.setNextAttemptAt(now.plus(properties.delayFor(triggerHaltCode)));
        row.setDeadlineAt(now.plus(properties.getGiveUpAfter()));
        try {
            DeferredRecovery saved = repository.saveAndFlush(row);
            LOG.info("[RECOVERY] tracking requestId '{}' paymentType='{}' declared={} trigger='{}' "
                            + "firstAttempt={} (grace {}) deadline={}",
                    requestId, paymentType, declaredOutcome, triggerHaltCode, saved.getNextAttemptAt(),
                    properties.delayFor(triggerHaltCode), saved.getDeadlineAt());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Another thread won the race on the unique constraint. Its row is as good as ours.
            LOG.debug("[RECOVERY] concurrent enqueue for requestId '{}' — using the existing row", requestId);
            return repository.findByRequestId(requestId).orElseThrow(() -> e);
        }
    }

    /**
     * Takes the lease on a row, in its own transaction.
     *
     * @return {@code true} if this instance now owns the row. {@code false} means somebody else got
     *         there first — the optimistic-lock failure is the point, not an error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long id, String instanceId) {
        Optional<DeferredRecovery> found = repository.findById(id);
        if (found.isEmpty()) {
            return false;
        }
        DeferredRecovery row = found.orElseThrow();
        if (row.isTerminal()) {
            return false;
        }
        Instant now = Instant.now();
        if (row.getState() == RecoveryModel.RecoveryState.IN_PROGRESS
                && row.getClaimedAt() != null
                && row.getClaimedAt().isAfter(now.minus(properties.getLeaseDuration()))
                && !instanceId.equals(row.getOwnerInstance())) {
            return false;
        }
        row.setState(RecoveryModel.RecoveryState.IN_PROGRESS);
        row.setOwnerInstance(instanceId);
        row.setClaimedAt(now);
        try {
            repository.saveAndFlush(row);
            return true;
        } catch (OptimisticLockingFailureException e) {
            LOG.debug("[RECOVERY] lost the claim race for id {}", id);
            return false;
        }
    }

    /** Schedules another attempt after the backoff, releasing the lease. */
    @Transactional
    public void reschedule(Long id, RecoveryModel.CbsState cbsState, RecoveryModel.Action action,
                           String phase, String detail) {
        repository.findById(id).ifPresent(row -> {
            int attempts = row.getAttempts() + 1;
            row.setAttempts(attempts);
            row.setLastCbsState(cbsState);
            row.setLastAction(action);
            row.setLastPhase(phase);
            row.setLastDetail(truncate(detail));
            row.setOwnerInstance(null);
            row.setClaimedAt(null);

            Instant now = Instant.now();
            Instant next = now.plus(properties.backoffFor(attempts));
            if (attempts >= properties.getMaxAttempts() || !next.isBefore(row.getDeadlineAt())) {
                // Out of road. Escalate rather than schedule an attempt that would never be eligible.
                row.setState(RecoveryModel.RecoveryState.NEEDS_ATTENTION);
                row.setNextAttemptAt(now);
                LOG.error("[RECOVERY] ESCALATED requestId '{}' after {} attempts — declared={} lastCbsState={} detail='{}'. "
                                + "Core banking does not match what the scheme was told; this needs a human.",
                        row.getRequestId(), attempts, row.getDeclaredOutcome(), cbsState, row.getLastDetail());
            } else {
                row.setState(RecoveryModel.RecoveryState.PENDING);
                row.setNextAttemptAt(next);
                LOG.info("[RECOVERY] requestId '{}' attempt {} → {} ({}), next attempt at {}",
                        row.getRequestId(), attempts, action, cbsState, next);
            }
            repository.save(row);
        });
    }

    /** Core banking now agrees with what the scheme was told. */
    @Transactional
    public void reconciled(Long id, RecoveryModel.CbsState cbsState, RecoveryModel.Action action,
                           String phase, String detail) {
        repository.findById(id).ifPresent(row -> {
            row.setAttempts(row.getAttempts() + 1);
            row.setState(RecoveryModel.RecoveryState.RECONCILED);
            row.setLastCbsState(cbsState);
            row.setLastAction(action);
            row.setLastPhase(phase);
            row.setLastDetail(truncate(detail));
            row.setOwnerInstance(null);
            row.setClaimedAt(null);
            repository.save(row);
            LOG.info("[RECOVERY] requestId '{}' reconciled after {} attempt(s) — declared={} cbs={} via {}",
                    row.getRequestId(), row.getAttempts(), row.getDeclaredOutcome(), cbsState, action);
        });
    }

    /** Escalates immediately, without spending the remaining attempts. */
    @Transactional
    public void escalate(Long id, String detail) {
        escalate(id, null, null, null, detail, false);
    }

    /**
     * Escalates a row whose <b>repair attempt actually ran</b> and came back definitively refused.
     *
     * <p>Distinct from {@link #escalate(Long, String)} because that one is used before any work
     * happens — an unresolvable declared outcome, a missing {@code payment_log} row — where there is
     * no attempt to count and no core-banking state to record.
     *
     * <p>Here there is, and it is the whole reason a human is being called: what the scheme was told,
     * what the gate found, and which repair core banking refused are exactly the three facts needed
     * to decide what to do, and none of them are recoverable from the free-text detail alone. Without
     * the trail the operator opens the row and finds a sentence.
     */
    @Transactional
    public void escalate(Long id, RecoveryModel.CbsState cbsState, RecoveryModel.Action action,
                         String phase, String detail) {
        escalate(id, cbsState, action, phase, detail, true);
    }

    private void escalate(Long id, RecoveryModel.CbsState cbsState, RecoveryModel.Action action,
                          String phase, String detail, boolean attemptRan) {
        repository.findById(id).ifPresent(row -> {
            if (attemptRan) {
                row.setAttempts(row.getAttempts() + 1);
                row.setLastCbsState(cbsState);
                row.setLastAction(action);
                row.setLastPhase(phase);
            }
            row.setState(RecoveryModel.RecoveryState.NEEDS_ATTENTION);
            row.setLastDetail(truncate(detail));
            row.setOwnerInstance(null);
            row.setClaimedAt(null);
            repository.save(row);
            LOG.error("[RECOVERY] ESCALATED requestId '{}' — {}", row.getRequestId(), detail);
        });
    }

    /**
     * Puts an escalated or cancelled row back in the queue, e.g. after core banking is fixed.
     * Resets the attempt budget and the deadline: the operator is asserting the situation changed.
     */
    @Transactional
    public Optional<DeferredRecovery> requeue(String requestId) {
        return repository.findByRequestId(requestId).map(row -> {
            Instant now = Instant.now();
            row.setState(RecoveryModel.RecoveryState.PENDING);
            row.setAttempts(0);
            row.setNextAttemptAt(now);
            row.setDeadlineAt(now.plus(properties.getGiveUpAfter()));
            row.setOwnerInstance(null);
            row.setClaimedAt(null);
            row.setLastDetail("Requeued by operator");
            LOG.warn("[RECOVERY] requestId '{}' requeued by operator", requestId);
            return repository.save(row);
        });
    }

    /**
     * Supplies the declared outcome for a row the pipeline could not establish one for, and puts it
     * back to work.
     *
     * <p>This is the only way an {@link RecoveryModel.DeclaredOutcome#UNKNOWN} row can ever be
     * reconciled: {@link #requeue} alone sends it round again and the engine escalates it again,
     * correctly, because requeuing supplies no new information. The missing fact is what the scheme
     * was told, and only a person who has read the callback that went out can provide it.
     *
     * <p><b>Restricted to UNKNOWN rows on purpose.</b> Overwriting an outcome the corridor already
     * established would let an operator drive a repair against a transaction whose answer is not in
     * doubt — reversing a credit the scheme was told had settled is a one-click mistake, and there is
     * no legitimate reason to correct a value the callback handler stamped from the message it
     * actually sent.
     *
     * @return the updated row, or empty when there is no such transaction
     * @throws IllegalStateException    if the row already has an established outcome
     * @throws IllegalArgumentException if asked to set UNKNOWN, which resolves nothing
     */
    @Transactional
    public Optional<DeferredRecovery> setDeclaredOutcome(String requestId,
                                                         RecoveryModel.DeclaredOutcome outcome,
                                                         String reason) {
        if (outcome == null || outcome == RecoveryModel.DeclaredOutcome.UNKNOWN) {
            throw new IllegalArgumentException(
                    "A declared outcome of UNKNOWN resolves nothing — supply SUCCESS, FAILED or PENDING");
        }
        return repository.findByRequestId(requestId).map(row -> {
            if (row.getDeclaredOutcome() != RecoveryModel.DeclaredOutcome.UNKNOWN) {
                throw new IllegalStateException("requestId '" + requestId + "' already has declared outcome "
                        + row.getDeclaredOutcome() + "; it is not in doubt and must not be overwritten");
            }
            Instant now = Instant.now();
            row.setDeclaredOutcome(outcome);
            row.setState(RecoveryModel.RecoveryState.PENDING);
            row.setAttempts(0);
            row.setNextAttemptAt(now);
            row.setDeadlineAt(now.plus(properties.getGiveUpAfter()));
            row.setOwnerInstance(null);
            row.setClaimedAt(null);
            row.setLastDetail(truncate("Declared outcome set to " + outcome + " by operator"
                    + (reason == null || reason.isBlank() ? "" : ": " + reason)));
            LOG.warn("[RECOVERY] requestId '{}' declared outcome set to {} by operator ({}) — reconciliation "
                    + "resumes", requestId, outcome, reason);
            return repository.save(row);
        });
    }

    /** What a scheme-initiated reversal should do about a transaction the reconciler may hold. */
    public enum SchemeReversal {
        /** No reconciler work stands in the way: the scheme's reversal runs as normal. */
        PROCEED,
        /**
         * The reconciler has already started backing this credit out, or has finished. The scheme's
         * reversal must be answered without moving money again.
         */
        HANDLED_BY_RECOVERY
    }

    /**
     * Arbitrates between the reconciler and a reversal the scheme sent itself (an R-NDPS pacs.007).
     *
     * <p>Both back out the same credit, and they cannot both be allowed to. For a CBS posting core
     * banking would refuse the second with {@code cbc-reversal-already-done}, but a DFS wallet credit
     * has no such guard: the pacs.007 carries its own conversation id, so DFS would debit the wallet
     * twice. This is the only thing that prevents that.
     *
     * <ul>
     *   <li><b>Declared FAILED, reconciler has not started</b> ({@code attempts == 0}, not leased) —
     *       the scheme's reversal arrived inside the grace period, which is the normal case. The row
     *       is cancelled as superseded and the reversal proceeds.</li>
     *   <li><b>Declared FAILED, reconciler has started or finished</b> — any attempt may already have
     *       sent a reversal, and only the reconciler can see its outcome through. The scheme's
     *       reversal is answered without moving money. Keyed on {@code attempts}, not on
     *       {@code lastAction}: a later attempt's {@code RETRY} overwrites the {@code REVERSE} that
     *       preceded it.</li>
     *   <li><b>Declared SUCCESS or PENDING</b> — a CBS-timeout row the reconciler would resolve by
     *       reposting. The scheme has now cancelled the transfer, so the target has flipped: the row
     *       is cancelled and the reversal proceeds. If that row is mid-attempt the reversal still
     *       proceeds — refusing a pacs.007 is not an option — and an ERROR asks a human to check.</li>
     *   <li><b>CANCELLED, or no row</b> — nothing to arbitrate.</li>
     * </ul>
     *
     * <p>Deliberately not {@code @Transactional}: the cancel is a conditional write on the row
     * version, and losing that race to a worker's claim must be caught here and read as "the
     * reconciler got there first", not surface as a rolled-back outer transaction.
     */
    public SchemeReversal handOverToScheme(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return SchemeReversal.PROCEED;
        }
        Optional<DeferredRecovery> found = repository.findByRequestId(requestId);
        if (found.isEmpty()) {
            return SchemeReversal.PROCEED;
        }
        DeferredRecovery row = found.orElseThrow();
        RecoveryModel.RecoveryState state = row.getState();
        if (state == RecoveryModel.RecoveryState.CANCELLED) {
            return SchemeReversal.PROCEED;
        }

        if (row.getDeclaredOutcome() != RecoveryModel.DeclaredOutcome.FAILED) {
            if (state == RecoveryModel.RecoveryState.RECONCILED) {
                return SchemeReversal.PROCEED;
            }
            if (isLeased(row)) {
                LOG.error("[RECOVERY] requestId '{}': scheme reversal arrived while the reconciler is working a "
                                + "declared={} row (attempt {}). The reversal proceeds; check core banking for a "
                                + "repost racing it.",
                        requestId, row.getDeclaredOutcome(), row.getAttempts() + 1);
                return SchemeReversal.PROCEED;
            }
            supersede(row, "Superseded by scheme reversal: the transfer was cancelled, declared "
                    + row.getDeclaredOutcome() + " no longer holds");
            return SchemeReversal.PROCEED;
        }

        if (row.getAttempts() > 0 || isLeased(row) || state == RecoveryModel.RecoveryState.RECONCILED) {
            LOG.info("[RECOVERY] requestId '{}': scheme reversal arrived after the reconciler started "
                            + "(state={}, attempts={}, lastAction={}) — answering it without reversing again",
                    requestId, state, row.getAttempts(), row.getLastAction());
            return SchemeReversal.HANDLED_BY_RECOVERY;
        }
        return supersede(row, "Superseded by scheme reversal inside the grace period")
                ? SchemeReversal.PROCEED
                : SchemeReversal.HANDLED_BY_RECOVERY;
    }

    private boolean isLeased(DeferredRecovery row) {
        return row.getState() == RecoveryModel.RecoveryState.IN_PROGRESS
                && row.getClaimedAt() != null
                && row.getClaimedAt().isAfter(Instant.now().minus(properties.getLeaseDuration()));
    }

    /** @return {@code false} when a worker claimed the row between the read and this write */
    private boolean supersede(DeferredRecovery row, String detail) {
        row.setState(RecoveryModel.RecoveryState.CANCELLED);
        row.setLastDetail(truncate(detail));
        row.setOwnerInstance(null);
        row.setClaimedAt(null);
        try {
            repository.saveAndFlush(row);
            LOG.info("[RECOVERY] requestId '{}' cancelled — {}", row.getRequestId(), detail);
            return true;
        } catch (OptimisticLockingFailureException e) {
            LOG.info("[RECOVERY] requestId '{}': the reconciler claimed the row as the scheme reversal "
                    + "arrived — leaving it to the reconciler", row.getRequestId());
            return false;
        }
    }

    /** Operator calls it off — the inconsistency is being settled outside the system. */
    @Transactional
    public Optional<DeferredRecovery> cancel(String requestId, String reason) {
        return repository.findByRequestId(requestId).map(row -> {
            row.setState(RecoveryModel.RecoveryState.CANCELLED);
            row.setLastDetail(truncate(reason));
            row.setOwnerInstance(null);
            row.setClaimedAt(null);
            LOG.warn("[RECOVERY] requestId '{}' cancelled by operator: {}", requestId, reason);
            return repository.save(row);
        });
    }

    @Transactional(readOnly = true)
    public List<DeferredRecovery> claimable(int limit) {
        Instant now = Instant.now();
        return repository.findClaimable(now, now.minus(properties.getLeaseDuration()),
                PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<DeferredRecovery> expired(int limit) {
        return repository.findExpired(Instant.now(), PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Optional<DeferredRecovery> find(String requestId) {
        return repository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public Page<DeferredRecovery> list(RecoveryModel.RecoveryState state, Pageable pageable) {
        return state == null ? repository.findAll(pageable) : repository.findByState(state, pageable);
    }

    /** Row counts per state — the numbers the monitoring endpoint and any alert rule read. */
    @Transactional(readOnly = true)
    public Map<RecoveryModel.RecoveryState, Long> counts() {
        Map<RecoveryModel.RecoveryState, Long> counts = new EnumMap<>(RecoveryModel.RecoveryState.class);
        for (RecoveryModel.RecoveryState state : RecoveryModel.RecoveryState.values()) {
            counts.put(state, repository.countByState(state));
        }
        return counts;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 497) + "...";
    }
}
