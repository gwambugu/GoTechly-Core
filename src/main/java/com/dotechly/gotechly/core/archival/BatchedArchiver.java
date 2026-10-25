package com.dotechly.gotechly.core.archival;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The move-then-delete loop shared by every archival job: select a page of ids, copy those rows to
 * the history table, delete them, commit, repeat until drained or out of time.
 *
 * <p>Extracted because the loop is four lines of obvious code wrapped around four decisions that are
 * not obvious at all, and each one is a way to lose rows or corrupt the history table. Two copies of
 * it would drift, and the drift would be silent.
 *
 * <h2>Why ids are selected first</h2>
 * The alternative — one {@code INSERT..SELECT WHERE <predicate>} followed by
 * {@code DELETE WHERE <predicate>} — re-evaluates the predicate twice. Any row that changes between
 * the two statements is seen by one and not the other, and the difference is rows deleted without
 * being archived. Selecting ids once and keying both statements on that list removes the question.
 *
 * <h2>Why each batch is its own transaction</h2>
 * A single transaction across the whole cutoff range has UNDO proportional to the table, holds locks
 * for its full duration, and on Oracle keeps a read open long enough to hit
 * {@code ORA-01555 snapshot too old}. It works right up until the table is big — which is the only
 * situation an archival job exists for. Committing per batch bounds all three, and a failure costs
 * one batch instead of the run.
 *
 * <p><b>The caller must not be {@code @Transactional}.</b> Spring's proxy is bypassed on
 * self-invocation, so a {@code @Transactional} method calling a {@code REQUIRES_NEW} method on the
 * same bean silently runs everything in one transaction — producing exactly the shape being avoided,
 * while looking correct. That is why this class owns a {@link TransactionTemplate} rather than
 * relying on annotations.
 *
 * <h2>Why there is a runtime budget</h2>
 * ShedLock's {@code lockAtMostFor} is <b>not</b> a timeout — it is the point after which another
 * instance is allowed to start the same job. A run that overshoots it does not get stopped; it gets
 * joined, and two instances then copy and delete the same rows. The budget must therefore be set
 * below the lease so the job yields first and finishes the remainder on the next run. A unique
 * constraint on the history table's natural key is the backstop if it ever does overlap.
 *
 * <h2>Oracle IN-list limit</h2>
 * Both statements are keyed on a list of ids, and Oracle rejects more than 1000 expressions in an
 * {@code IN} list with {@code ORA-01795}. Oversized batch sizes are clamped rather than rejected:
 * refusing to start over a tuning value would be worse than quietly doing the right thing, and the
 * clamp is logged.
 */
@Component
public class BatchedArchiver {

    private static final Logger LOG = LoggerFactory.getLogger(BatchedArchiver.class);

    /** {@code ORA-01795: maximum number of expressions in a list is 1000}. */
    public static final int MAX_BATCH_SIZE = 1000;

    private final TransactionTemplate transactionTemplate;

    public BatchedArchiver(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * The three table-specific statements. Everything else about an archival is the same.
     *
     * <p>{@code archivedAt} is passed to {@link #copy} for tables that record when the move happened
     * as distinct from when the row settled; implementations whose history table has no such column
     * ignore it.
     */
    public interface ArchivalStatements {
        List<Long> findIds(Instant cutoff, int batchSize);

        int copy(List<Long> ids, Instant archivedAt);

        int delete(List<Long> ids);
    }

    /**
     * What a run did. {@code ranOutOfTime} and {@code failed} are separate because they call for
     * different responses: the first is normal on a backlog and self-corrects on the next run, the
     * second means something is wrong now.
     */
    public record ArchivalResult(long archived, int batches, boolean ranOutOfTime, boolean failed) {
        public boolean archivedNothing() {
            return archived == 0;
        }
    }

    public ArchivalResult run(String jobName, Instant cutoff, int configuredBatchSize,
                              Duration maxRuntime, ArchivalStatements statements) {
        int batchSize = Math.min(Math.max(configuredBatchSize, 1), MAX_BATCH_SIZE);
        if (batchSize != configuredBatchSize) {
            LOG.warn("[ARCHIVAL:{}] batch size {} clamped to {} — the move is keyed on an IN list and "
                            + "Oracle rejects more than {} expressions (ORA-01795)",
                    jobName, configuredBatchSize, batchSize, MAX_BATCH_SIZE);
        }

        Instant deadline = Instant.now().plus(maxRuntime);
        LOG.info("[ARCHIVAL:{}] starting: cutoff={} batch={} budget={}",
                jobName, cutoff, batchSize, maxRuntime);

        long archived = 0;
        int batches = 0;
        boolean ranOutOfTime = false;
        boolean failed = false;

        while (true) {
            // >= deadline, not > deadline: at the instant the budget runs out it is already spent,
            // and a coarse clock can otherwise let one more batch through on the boundary.
            if (!Instant.now().isBefore(deadline)) {
                ranOutOfTime = true;
                break;
            }
            Integer moved = runOneBatch(jobName, cutoff, batchSize, statements);
            if (moved == null) {
                failed = true;
                break;
            }
            if (moved == 0) {
                break;
            }
            archived += moved;
            batches++;
        }

        if (ranOutOfTime) {
            LOG.warn("[ARCHIVAL:{}] stopped after {} rows in {} batches — hit the {} budget with work "
                            + "remaining. The rest goes on the next run. If this repeats nightly the "
                            + "table is growing faster than it drains: raise the budget (keeping it "
                            + "below the ShedLock lease) or shorten the retention.",
                    jobName, archived, batches, maxRuntime);
        } else if (failed) {
            LOG.error("[ARCHIVAL:{}] ended early after {} rows in {} batches — see the batch error above",
                    jobName, archived, batches);
        } else {
            LOG.info("[ARCHIVAL:{}] archived {} rows in {} batches", jobName, archived, batches);
        }
        return new ArchivalResult(archived, batches, ranOutOfTime, failed);
    }

    /**
     * One batch in its own transaction. Returns rows moved, or {@code null} if the batch failed —
     * which ends the run rather than retrying, because the next attempt selects the same ids and
     * fails the same way, and the scheduler will be back.
     *
     * <p>The {@code copied != deleted} check should be unreachable: both statements are keyed on the
     * same explicit id list inside one transaction. It is here because a mismatch is the one outcome
     * worth aborting for — rows deleted without being archived — and because an archival that
     * quietly does the wrong thing is this codebase's established failure mode.
     */
    private Integer runOneBatch(String jobName, Instant cutoff, int batchSize,
                                ArchivalStatements statements) {
        try {
            return transactionTemplate.execute(status -> {
                List<Long> ids = statements.findIds(cutoff, batchSize);
                if (ids.isEmpty()) {
                    return 0;
                }
                Instant archivedAt = Instant.now();
                int copied = statements.copy(ids, archivedAt);
                int deleted = statements.delete(ids);
                if (copied != deleted) {
                    status.setRollbackOnly();
                    LOG.error("[ARCHIVAL:{}] batch mismatch: selected={} copied={} deleted={} — rolled "
                                    + "back. Rows would have been deleted without being archived.",
                            jobName, ids.size(), copied, deleted);
                    return 0;
                }
                return copied;
            });
        } catch (Exception e) {
            LOG.error("[ARCHIVAL:{}] batch failed: {}", jobName, e.getMessage(), e);
            return null;
        }
    }
}
