package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.node;

import com.dotechly.gotechly.core.config.InstanceInfo;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.Stan;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.StanGenerator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Claims this instance's DE11 node ordinal and keeps it.
 *
 * <p>{@link StanGenerator} splits the six-digit STAN space into ten disjoint blocks and needs to know
 * which one is ours. Everything hard about that is the <em>identity</em>, not the arithmetic: a
 * config property would work right up until someone copied a config file between instances, at which
 * point two of them share a block, the STANs collide, and - because every mux keys on
 * {@code (DE12, DE37)} and the RRN embeds the STAN - one transaction starts receiving another's
 * core-banking response. Nothing would detect it. A claimed row cannot be copied.
 *
 * <p><b>No new configuration.</b> Ownership keys on {@link InstanceInfo#getInstanceId()} -
 * {@code hostname:server.port} - which is already unique per instance and already exists. A restart
 * of the same instance reclaims the same ordinal, so ordinals stay stable across deploys.
 *
 * <p><b>Off the hot path.</b> One conditional UPDATE at startup and one every {@link #HEARTBEAT}
 * after that. Generating a STAN touches nothing but an {@code AtomicLong}.
 */
@Service
public class StanNodeLeaseService {

    private static final Logger LOG = LoggerFactory.getLogger(StanNodeLeaseService.class);

    /**
     * How often the owner proves it is alive.
     *
     * <p>Constants rather than properties on purpose: these are not a deployment's business, and the
     * one thing that actually varies - which ordinal we get - is decided by the table.
     */
    static final Duration HEARTBEAT = Duration.ofSeconds(30);

    /**
     * How long an ordinal survives without a heartbeat before another instance may take it.
     *
     * <p>Ten missed heartbeats. Generous on purpose: reclaiming too eagerly has two live instances
     * briefly sharing a block, while reclaiming too slowly only leaves a dead instance's ordinal idle
     * among ten for a few minutes.
     */
    static final Duration LEASE_TTL = Duration.ofMinutes(5);

    private final StanNodeLeaseStore store;
    private final InstanceInfo instanceInfo;

    private volatile int nodeId = -1;

    public StanNodeLeaseService(StanNodeLeaseStore store, InstanceInfo instanceInfo) {
        this.store = store;
        this.instanceInfo = instanceInfo;
    }

    /**
     * Runs during context refresh, so it completes before {@code Q2Lifecycle} (phase
     * {@code Integer.MAX_VALUE}) deploys anything that could ask for a STAN.
     *
     * <p><b>Refuses startup when no ordinal can be claimed.</b> The alternative - start anyway and
     * emit STANs from an unowned block - is the collision this mechanism exists to remove, and it
     * would be invisible. Core already cannot start without the database (Liquibase runs first), so
     * this adds no new reason to fail; the only genuinely new case is all ten ordinals being held by
     * live instances, which means an eleventh instance and wants a human either way.
     */
    @PostConstruct
    public void claim() {
        String owner = instanceInfo.getInstanceId();
        Instant now = Instant.now();

        Optional<Integer> mine = store.reclaimOwn(owner, now);
        if (mine.isPresent()) {
            install(mine.orElseThrow(), owner, "reclaimed");
            return;
        }

        Instant staleBefore = now.minus(LEASE_TTL);
        for (int candidate = 0; candidate < StanGenerator.NODES; candidate++) {
            if (store.claim(candidate, owner, now, staleBefore)) {
                install(candidate, owner, "claimed");
                return;
            }
        }
        throw new IllegalStateException(
                "No free STAN node ordinal in cbs_node_lease for instance " + owner + ". All "
                        + StanGenerator.NODES + " blocks are held by instances that have checked in "
                        + "within " + LEASE_TTL + ". Either more Core instances are running than the "
                        + "STAN space allows, or rows are held by instances that no longer exist - "
                        + "check the table and clear the dead owners.");
    }

    /**
     * Proof of life, and the only thing that notices the ordinal being taken away.
     *
     * <p><b>Deliberately no {@code @SchedulerLock}.</b> The project rule is that a {@code @Scheduled}
     * without one fires on every instance; here that is the entire point - every instance has its own
     * lease to refresh. A lock would let one instance renew while the other two went stale and had
     * their blocks reassigned underneath them.
     */
    @Scheduled(fixedDelayString = "PT30S")
    public void heartbeat() {
        int current = nodeId;
        if (current < 0) {
            return;
        }
        String owner = instanceInfo.getInstanceId();
        Instant now = Instant.now();
        if (store.heartbeat(current, owner, now) > 0) {
            return;
        }
        // The row is no longer ours: we were declared stale and another instance took the block.
        // Continuing to emit from it is the collision, so move rather than carry on.
        LOG.error("[STAN] lost node ordinal {} (owner {}) - another instance took it after this one "
                + "missed heartbeats for {}. Claiming a different block.", current, owner, LEASE_TTL);
        Instant staleBefore = now.minus(LEASE_TTL);
        for (int candidate = 0; candidate < StanGenerator.NODES; candidate++) {
            if (candidate != current && store.claim(candidate, owner, now, staleBefore)) {
                install(candidate, owner, "re-claimed after losing " + current);
                return;
            }
        }
        // Nothing free. Keeping the current generator is wrong, but refusing to emit a STAN would
        // stop every posting on this instance, and a shared block is the lesser of those two. It is
        // logged at ERROR precisely because only a human can resolve it.
        LOG.error("[STAN] no free node ordinal to move to - instance {} is still emitting STANs from "
                        + "block {}, which another instance now also owns. Duplicate STANs are "
                        + "possible until this is resolved; drain or restart one of them.",
                owner, current);
    }

    /**
     * Gives the ordinal back on a clean shutdown so a redeploy does not have to wait out
     * {@link #LEASE_TTL}. Best effort - an abrupt kill is covered by the stale window.
     */
    @PreDestroy
    public void release() {
        if (nodeId < 0) {
            return;
        }
        try {
            store.releaseAllOwnedBy(instanceInfo.getInstanceId());
            LOG.info("[STAN] released node ordinal {} on shutdown", nodeId);
        } catch (RuntimeException e) {
            LOG.warn("[STAN] could not release node ordinal {} on shutdown (it expires after {}): {}",
                    nodeId, LEASE_TTL, e.getMessage());
        }
    }

    /** The ordinal in force, or -1 before startup completed. */
    public int nodeId() {
        return nodeId;
    }

    private void install(int candidate, String owner, String how) {
        this.nodeId = candidate;
        StanGenerator generator = new StanGenerator(candidate);
        Stan.install(generator);
        LOG.info("[STAN] instance {} {} node ordinal {} - DE11 range {}",
                owner, how, candidate, generator.range());
    }
}
