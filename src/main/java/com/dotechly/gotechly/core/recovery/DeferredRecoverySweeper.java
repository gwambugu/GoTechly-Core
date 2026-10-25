package com.dotechly.gotechly.core.recovery;

import com.dotechly.gotechly.core.config.drain.NodeDrainService;
import com.dotechly.gotechly.core.domain.DeferredRecovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Brings durable rows back into the in-memory working set, and escalates the ones that have run out
 * of time.
 *
 * <p>This is the component that makes the module survive its own environment. It covers four cases
 * that the queue alone cannot:
 * <ul>
 *   <li><b>Restart or deploy</b> — the queue is empty, the rows are not.</li>
 *   <li><b>Queue at capacity</b> — {@code offer} deferred the row rather than rejecting it.</li>
 *   <li><b>Instance death mid-attempt</b> — the row sits {@code IN_PROGRESS} under a lease that has
 *       since expired, and another instance picks it up.</li>
 *   <li><b>Deadline passed</b> — escalated without spending a worker on an attempt that is no longer
 *       allowed to succeed.</li>
 * </ul>
 *
 * <p><b>Deliberately not {@code @SchedulerLock}ed.</b> Every other {@code @Scheduled} job in this
 * codebase carries one because it must run once per cluster; this one must run on <i>every</i>
 * instance, because its job is to fill that instance's own in-memory queue. Cluster-wide safety comes
 * from the per-row lease in {@link DeferredRecoveryService#claim}, one layer down, where it belongs:
 * a lock here would leave every instance but one with an empty working set.
 */
@Component
public class DeferredRecoverySweeper {

    private static final Logger LOG = LoggerFactory.getLogger(DeferredRecoverySweeper.class);

    private final RecoveryProperties properties;
    private final DeferredRecoveryService service;
    private final DeferredRecoveryQueue queue;
    private final NodeDrainService drainService;

    public DeferredRecoverySweeper(RecoveryProperties properties, DeferredRecoveryService service,
                                   DeferredRecoveryQueue queue, NodeDrainService drainService) {
        this.properties = properties;
        this.service = service;
        this.queue = queue;
        this.drainService = drainService;
    }

    /**
     * <p><b>Skipped while this node is draining.</b> A sweep pulls rows into the local working set
     * for the workers to repair, and a repair runs plugin handler stages — the very plugin an
     * operator is about to swap. Skipping is free here for the same reason the missing
     * {@code @SchedulerLock} is safe: every instance sweeps, so a draining node's peers keep the
     * rows moving, and a row this node declines to pick up is simply claimed elsewhere. The next
     * cycle 60s later covers anything nobody took.
     */
    @Scheduled(fixedDelayString = "${gotechly.recovery.sweep-interval:PT60S}",
            initialDelayString = "${gotechly.recovery.sweep-interval:PT60S}")
    public void sweep() {
        if (!properties.isEnabled()) {
            return;
        }
        drainService.runIfRoutable("deferred-recovery-sweep", () -> {
            try {
                escalateExpired();
                rehydrate();
            } catch (Exception e) {
                LOG.error("[RECOVERY] sweep failed: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Past the deadline and still inconsistent. Escalated here rather than on the next attempt so a
     * row cannot sit waiting out a long backoff that would expire before it ever ran again.
     */
    private void escalateExpired() {
        List<DeferredRecovery> expired = service.expired(properties.getSweepBatchSize());
        for (DeferredRecovery row : expired) {
            service.escalate(row.getId(), "Deadline " + row.getDeadlineAt() + " passed after "
                    + row.getAttempts() + " attempt(s); declared=" + row.getDeclaredOutcome()
                    + " lastCbsState=" + row.getLastCbsState());
        }
        if (!expired.isEmpty()) {
            LOG.error("[RECOVERY] {} transaction(s) escalated to NEEDS_ATTENTION on deadline — core banking "
                    + "does not match what the scheme was told", expired.size());
        }
    }

    private void rehydrate() {
        List<DeferredRecovery> due = service.claimable(properties.getSweepBatchSize());
        int offered = 0;
        for (DeferredRecovery row : due) {
            if (queue.offer(row)) {
                offered++;
            }
        }
        if (offered > 0) {
            LOG.info("[RECOVERY] swept {} due row(s) into the working set ({} candidate(s) examined)",
                    offered, due.size());
        }
    }
}
