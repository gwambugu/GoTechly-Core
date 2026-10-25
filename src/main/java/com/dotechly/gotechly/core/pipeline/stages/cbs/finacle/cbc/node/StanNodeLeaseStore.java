package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.node;

import com.dotechly.gotechly.core.domain.CbsNodeLease;
import com.dotechly.gotechly.core.repository.CbsNodeLeaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The transactional half of the node lease.
 *
 * <p><b>A separate bean, and that is required rather than tidy.</b> {@link StanNodeLeaseService}
 * drives these from {@code @PostConstruct} and from a scheduler thread, calling several in a loop.
 * Were they methods on that class, every call would be a self-invocation, Spring's proxy would be
 * bypassed and {@code @Transactional} would be silently ignored - which for a {@code @Modifying}
 * bulk update means {@code TransactionRequiredException}, or worse, on a path that has an ambient
 * transaction, all ten claim attempts sharing one. Crossing a bean boundary is what makes the
 * annotations mean anything.
 *
 * <p>Each method is {@code REQUIRES_NEW} so one failed claim rolls back alone and the caller can try
 * the next ordinal.
 */
@Component
public class StanNodeLeaseStore {

    private static final Logger LOG = LoggerFactory.getLogger(StanNodeLeaseStore.class);

    private final CbsNodeLeaseRepository repository;

    public StanNodeLeaseStore(CbsNodeLeaseRepository repository) {
        this.repository = repository;
    }

    /**
     * Takes an ordinal if it is free, already ours, or held by an instance that stopped checking in.
     *
     * <p>Swallows a failure into {@code false} rather than propagating: the caller is walking
     * candidates and a lost race is an ordinary outcome, not an error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(int nodeId, String owner, Instant now, Instant staleBefore) {
        try {
            return repository.claim(nodeId, owner, now, staleBefore) > 0;
        } catch (RuntimeException e) {
            LOG.debug("[STAN] claim of ordinal {} failed: {}", nodeId, e.getMessage());
            return false;
        }
    }

    /**
     * Takes back the ordinal this instance held before it restarted, which is what keeps ordinals
     * stable across deploys. The staleness bound is {@code now} - no bound at all - because the row
     * already names us and there is nothing to wait for.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Integer> reclaimOwn(String owner, Instant now) {
        List<CbsNodeLease> existing = repository.findByOwnerOrderByNodeIdAsc(owner);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        // Normally exactly one. Two can only be left by a crash between claiming a new ordinal and
        // releasing the old one, and the right answer is to keep the lowest rather than to fail
        // startup over it - so the extras go back in the pool first.
        int nodeId = existing.get(0).getNodeId();
        if (existing.size() > 1) {
            LOG.warn("[STAN] instance {} names {} ordinals {} - keeping {} and releasing the rest",
                    owner, existing.size(),
                    existing.stream().map(CbsNodeLease::getNodeId).toList(), nodeId);
            repository.releaseAllOwnedBy(owner);
        }
        return repository.claim(nodeId, owner, now, now) > 0 ? Optional.of(nodeId) : Optional.empty();
    }

    /** Zero means the ordinal is no longer ours. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int heartbeat(int nodeId, String owner, Instant now) {
        return repository.heartbeat(nodeId, owner, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseAllOwnedBy(String owner) {
        repository.releaseAllOwnedBy(owner);
    }

    /** For the admin view and for diagnosing a duplicate-STAN report. */
    @Transactional(readOnly = true)
    public List<CbsNodeLease> all() {
        return repository.findAllOrdered();
    }
}
