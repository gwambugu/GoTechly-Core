package com.dotechly.gotechly.core.recovery;

import com.dotechly.gotechly.core.archival.BatchedArchiver;
import com.dotechly.gotechly.core.archival.RetentionSettings;
import com.dotechly.gotechly.core.config.drain.NodeDrainService;
import com.dotechly.gotechly.core.repository.DeferredRecoveryRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Moves settled reconciliation rows out of {@code deferred_recovery} into
 * {@code history_deferred_recovery}.
 *
 * <p>The live table is a work queue: {@code DeferredRecoverySweeper} scans it on
 * {@code (state, next_attempt_at)} every 60 seconds, and nothing ever deleted from it, so it grew
 * with every transaction ever reconciled and that scan got slower forever. Archiving finished rows
 * keeps it the size of the work actually outstanding.
 *
 * <h2>What is archivable is decided by state, not by age</h2>
 * Only {@code RECONCILED} and {@code CANCELLED}. This is the difference between this job and the
 * {@code audit_log} one, which is append-only and can archive purely on age:
 * <ul>
 *   <li>{@code PENDING} / {@code IN_PROGRESS} are live work at any age. A row pending for a
 *       fortnight means a corridor has been down for a fortnight — archiving it would delete work
 *       the reconciler is still doing.</li>
 *   <li>{@code NEEDS_ATTENTION} is an <b>open money position</b>: a scheme holds an answer core
 *       banking does not match. Its only visibility is the count on
 *       {@code GET /api/v1/admin/recovery/metrics}, so archiving it would take that count to zero
 *       and make an unresolved position look settled. It is excluded at every retention, and there
 *       is deliberately no config key to include it.</li>
 * </ul>
 *
 * <p>Age then narrows that set further, and the window is the {@code app_configs} row
 * {@value RetentionSettings#DEFERRED_RECOVERY_RETENTION}, read fresh at the start of every run so
 * one value governs the cluster. It can only ever make the job archive less than state already
 * allows — no value in that row reaches an open position.
 *
 * <p>Batching, per-batch transactions, the id-keyed move and the runtime budget all live in
 * {@link BatchedArchiver}, which explains why each of them is load-bearing.
 *
 * <h2>Oracle: DELETE does not give the space back</h2>
 * The segment keeps its high-water mark, so a table that has been drained still full-scans the same
 * number of (now empty) blocks. After the first large archival run, reclaim it once:
 *
 * <pre>
 *   ALTER TABLE deferred_recovery ENABLE ROW MOVEMENT;
 *   ALTER TABLE deferred_recovery SHRINK SPACE COMPACT;   -- online, incremental
 *   ALTER TABLE deferred_recovery SHRINK SPACE;           -- brief lock, moves the HWM
 *   ALTER INDEX idx_def_rec_state_next REBUILD ONLINE;    -- and the other three
 * </pre>
 *
 * Steady-state runs do not need it: once the table is small, the blocks freed each night are reused
 * by the next night's inserts.
 */
@Service
public class DeferredRecoveryArchivalService {

    private static final Logger LOG = LoggerFactory.getLogger(DeferredRecoveryArchivalService.class);

    private final DeferredRecoveryRepository repository;
    private final BatchedArchiver archiver;
    private final RetentionSettings retentionSettings;
    private final NodeDrainService drainService;

    /**
     * Fallback for the retention window, in days. The live value is the {@code app_configs} row
     * {@value RetentionSettings#DEFERRED_RECOVERY_RETENTION} — one row every node reads, changed
     * without a deploy — and this is only what the job uses when that row is missing or unusable.
     *
     * <p>It bounds <b>age</b> only. State decides what is archivable at all, and no value here can
     * reach a {@code PENDING}, {@code IN_PROGRESS} or {@code NEEDS_ATTENTION} row.
     */
    @Value("${gotechly.eod.deferred-recovery-retention-days:30}")
    private int retentionDaysFallback;

    @Value("${gotechly.eod.archive-batch-size:500}")
    private int batchSize;

    /** Must stay below the PT30M ShedLock lease, or a second instance can join the run. */
    @Value("${gotechly.eod.archive-max-runtime:PT20M}")
    private Duration maxRuntime;

    public DeferredRecoveryArchivalService(DeferredRecoveryRepository repository, BatchedArchiver archiver,
                                          RetentionSettings retentionSettings, NodeDrainService drainService) {
        this.repository = repository;
        this.archiver = archiver;
        this.retentionSettings = retentionSettings;
        this.drainService = drainService;
    }

    /**
     * Runs at 00:10, after the payment-log (00:00) and audit-log (00:05) archivals, so the three
     * never contend for the same undo and redo at once.
     *
     * <p>Deliberately not {@code @Transactional}: {@link BatchedArchiver} commits per batch, and an
     * outer transaction would enlist every one of them and undo the point of batching.
     */
    @Scheduled(cron = "0 10 0 * * ?")
    @SchedulerLock(name = "eod-deferred-recovery-archival", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void archiveSettledRows() {
        // Tracked, never gated — see EodService.runEodProcess() for why a ShedLock-ed job must not
        // decline work on a draining node.
        drainService.trackBackgroundJob("eod-deferred-recovery-archival", this::archiveSettled);
    }

    private void archiveSettled() {
        // Read per run, not at startup: the window is an operational dial, and a node that cached it
        // at boot would keep archiving on the old one until someone restarted it.
        int retentionDays = retentionSettings.days(RetentionSettings.DEFERRED_RECOVERY_RETENTION, retentionDaysFallback);
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        LOG.info("[ARCHIVAL:deferred_recovery] starting — retention {} day(s), cutoff {}.", retentionDays, cutoff);

        BatchedArchiver.ArchivalResult result = archiver.run("deferred_recovery", cutoff, batchSize,
                maxRuntime, new BatchedArchiver.ArchivalStatements() {
                    @Override
                    public List<Long> findIds(Instant c, int size) {
                        return repository.findArchivableIds(c, size);
                    }

                    @Override
                    public int copy(List<Long> ids, Instant archivedAt) {
                        return repository.copyToHistory(ids, archivedAt);
                    }

                    @Override
                    public int delete(List<Long> ids) {
                        return repository.deleteByIds(ids);
                    }
                });

        if (result.archivedNothing() && !result.failed()) {
            // "Archived 0" and "nothing left to archive" look identical in the count alone, and that
            // ambiguity is what hid a broken payment_log EOD for months. Say which one this was.
            long open = repository.countUnarchivable();
            LOG.info("[ARCHIVAL:deferred_recovery] nothing settled was older than the {}-day cutoff; "
                            + "{} row(s) remain open (PENDING / IN_PROGRESS / NEEDS_ATTENTION are "
                            + "never archived)", retentionDays, open);
        }
    }
}
