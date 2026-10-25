package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.config.drain.NodeDrainService;
import com.dotechly.gotechly.core.archival.BatchedArchiver;
import com.dotechly.gotechly.core.archival.RetentionSettings;
import com.dotechly.gotechly.core.repository.AuditLogRepository;
import com.dotechly.gotechly.core.repository.PaymentLogRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.Duration;
import java.util.List;
@Service
public class EodService {

    private final Logger log = LoggerFactory.getLogger(EodService.class);

    private final PaymentLogRepository paymentLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final BatchedArchiver archiver;
    private final RetentionSettings retentionSettings;
    private final NodeDrainService drainService;

    /**
     * Fallback for the audit-log retention window, in days. The live value is the {@code app_configs}
     * row {@value RetentionSettings#AUDIT_LOG_RETENTION}; this is only what the job uses when that
     * row is missing or unusable.
     */
    @Value("${gotechly.eod.audit-log-retention-days:14}")
    private int auditLogRetentionDaysFallback;

    /**
     * Fallback for the payment-log retention window, in days. The live value is the
     * {@code app_configs} row {@value RetentionSettings#PAYMENT_LOG_RETENTION} — one row every node
     * reads — and this is only what the job uses when that row is missing or unusable.
     *
     * <p>Defaults to {@code 0}, the original behaviour, where the cutoff is the run instant and the
     * job drains everything it can see. Any positive value keeps that many days of traffic in
     * {@code payment_log}, which is what the portal's live view, {@code db-logger}'s status lookup
     * and the receiver-name cache read before falling back to the history and pending twins.
     */
    @Value("${gotechly.eod.payment-log-retention-days:0}")
    private int paymentLogRetentionDaysFallback;

    @Value("${gotechly.eod.archive-batch-size:500}")
    private int archiveBatchSize;

    /** Must stay below the PT30M ShedLock lease, or a second instance can join the run. */
    @Value("${gotechly.eod.archive-max-runtime:PT20M}")
    private Duration archiveMaxRuntime;

    public EodService(PaymentLogRepository paymentLogRepository, AuditLogRepository auditLogRepository,
                      BatchedArchiver archiver, RetentionSettings retentionSettings,
                      NodeDrainService drainService) {
        this.paymentLogRepository = paymentLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.archiver = archiver;
        this.retentionSettings = retentionSettings;
        this.drainService = drainService;
    }

    /**
     * Moves settled {@code payment_log} rows into {@code history_payment_log} and everything else
     * into {@code pending_payment_log}, then deletes what it moved. Runs at midnight.
     *
     * <p><b>Status picks the destination, not eligibility.</b> The two copy predicates are exact
     * complements — {@code COMPLETED/FAILED/RESOLVED} (either case) to history, anything else
     * including NULL to pending — so every selected row lands in exactly one of them and the two
     * counts always sum to the number deleted.
     *
     * <p><b>Batched, and deliberately not {@code @Transactional}.</b> This is the heaviest of the
     * three archivals by a distance: ~95 columns including several CLOBs ({@code cbs_req}, the four
     * {@code *_additional_fields}, {@code psp_request} / {@code psp_response}), and three statements
     * per row rather than two. Doing that for a whole night's traffic in one transaction is the
     * shape most exposed to UNDO growth, to {@code ORA-01555} on a long read, and to overrunning the
     * {@code PT30M} lease — after which ShedLock lets a second instance start the same job and both
     * copy and delete the same rows. {@link BatchedArchiver} commits per batch; the annotation had to
     * go with it, because an outer transaction would enlist every batch and restore exactly the shape
     * being avoided.
     *
     * <p><b>Retention is age, and age alone decides eligibility.</b> The cutoff is
     * {@code now - <window>}, where the window comes from the {@code app_configs} row
     * {@value #PAYMENT_LOG_RETENTION_CONFIG_ID} — one value for the whole cluster, changed without a
     * deploy — and falls back to {@code gotechly.eod.payment-log-retention-days} only when that row is
     * missing or unusable. 0 is the original behaviour, where the job drains everything it can see.
     * Setting it keeps that many days of traffic in the live table, at the cost of the portal's live
     * view now spanning the window rather than the day.
     * It does <b>not</b> mean "settled that long ago": eligibility is {@code created_at}, the column
     * the index and the other two jobs already key on, so a transaction created outside the window is
     * archived even if a late status callback touched it yesterday. Live traffic landing mid-run is
     * outside the cutoff either way and waits for the next EOD.
     *
     * <p>A batch whose copies do not account for every row it is about to delete <b>rolls back</b>,
     * where the previous single-transaction version could only log a warning and carry on — rolling
     * back would have cost it the whole night's archived rows. Per batch there is nothing to lose:
     * the rows simply stay in {@code payment_log} for the next run.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @SchedulerLock(name = "eod-payment-log", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void runEodProcess() {
        // Tracked, never gated. ShedLock acquires the cluster lock BEFORE this body runs, so a
        // draining instance that declined here would still hold the lock (lockAtLeastFor = PT1M)
        // while its peers, firing on the same cron instant, have already failed to take it — the
        // night's run would be lost rather than moved. A drain simply waits for it.
        drainService.trackBackgroundJob("eod-payment-log", this::archivePaymentLog);
    }

    private void archivePaymentLog() {
        // Read per run, not at startup: the window is an operational dial, and a node that cached it
        // at boot would keep archiving on the old one until someone restarted it.
        int retentionDays = retentionSettings.days(RetentionSettings.PAYMENT_LOG_RETENTION, paymentLogRetentionDaysFallback);
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("EOD payment_log archival starting — retention {} day(s), cutoff {}.", retentionDays, cutoff);

        BatchedArchiver.ArchivalResult result = archiver.run("payment_log", cutoff, archiveBatchSize,
                archiveMaxRuntime, new BatchedArchiver.ArchivalStatements() {
                    @Override
                    public List<Long> findIds(Instant c, int batchSize) {
                        return paymentLogRepository.findArchivableIds(c, batchSize);
                    }

                    @Override
                    public int copy(List<Long> ids, Instant archivedAt) {
                        // Two destinations, one number: the archiver compares the total copied with
                        // the total deleted, which is exactly the invariant the old per-run
                        // reconciliation check was trying to assert.
                        int history = paymentLogRepository.copyToHistory(ids);
                        int pending = paymentLogRepository.copyToPending(ids);
                        return history + pending;
                    }

                    @Override
                    public int delete(List<Long> ids) {
                        return paymentLogRepository.deleteByIds(ids);
                    }
                });

        if (result.archivedNothing() && !result.failed()) {
            // A no-op EOD looks exactly like a healthy one in the counts alone. Every predicate is
            // created_at <= cutoff, and NULL <= cutoff is UNKNOWN, so rows that never got the stamp
            // are skipped silently and forever. That is how payment_log grew unbounded while EOD
            // reported success nightly.
            long unarchivable = paymentLogRepository.countUnarchivable();
            if (unarchivable > 0) {
                log.error("EOD archived nothing but {} payment_log rows have a NULL created_at "
                                + "and can never be archived. Run the one-off backfill "
                                + "(UPDATE payment_log SET created_at = request_timestamp WHERE created_at IS NULL). "
                                + "If the count keeps growing, a writer is bypassing JPA lifecycle callbacks.",
                        unarchivable);
            } else {
                log.info("EOD archived nothing and no rows have a NULL created_at — "
                        + "payment_log holds nothing older than its {}-day retention.", retentionDays);
            }
        }
    }


    /**
     * Archives {@code audit_log} rows past the retention window into {@code history_audit_log},
     * nightly at 00:05 — five minutes after the payment-log EOD, so the two never contend for the
     * same undo and redo at once.
     *
     * <p><b>Batched, and deliberately not {@code @Transactional}.</b> It used to move the whole
     * cutoff range in a single transaction, which is fine on a small table and progressively worse on
     * a large one: UNDO grows with the batch, locks are held throughout, and on Oracle the read is
     * eventually open long enough for {@code ORA-01555}. {@link BatchedArchiver} commits per batch.
     * The annotation has to go with it — an outer transaction would enlist every batch and undo the
     * whole point.
     *
     * <p>The first successful run carries however much backlog accumulated while this job was
     * failing on {@code ORA-01400} (see {@code AuditLogRepository.copyToHistory}), so it is likely to
     * exhaust its budget and finish over several nights. That is the intended behaviour, not a fault.
     *
     * <p>Unlike {@code deferred_recovery}, age is the whole criterion here: {@code audit_log} is
     * append-only, so there is no such thing as a row still being worked on.
     *
     * <p>The window is the {@code app_configs} row {@value RetentionSettings#AUDIT_LOG_RETENTION},
     * read fresh at the start of every run so one value governs the cluster, falling back to
     * {@code gotechly.eod.audit-log-retention-days}.
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @SchedulerLock(name = "eod-audit-log", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void runAuditLogArchival() {
        // Tracked, never gated — see runEodProcess().
        drainService.trackBackgroundJob("eod-audit-log", this::archiveAuditLog);
    }

    private void archiveAuditLog() {
        // Read per run, not at startup — see archivePaymentLog().
        int retentionDays = retentionSettings.days(RetentionSettings.AUDIT_LOG_RETENTION, auditLogRetentionDaysFallback);
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Audit archival starting — retention {} day(s), cutoff {}.", retentionDays, cutoff);

        BatchedArchiver.ArchivalResult result = archiver.run("audit_log", cutoff, archiveBatchSize,
                archiveMaxRuntime, new BatchedArchiver.ArchivalStatements() {
                    @Override
                    public List<Long> findIds(Instant c, int batchSize) {
                        return auditLogRepository.findArchivableIds(c, batchSize);
                    }

                    @Override
                    public int copy(List<Long> ids, Instant archivedAt) {
                        // history_audit_log has no archived_at column; created_at is the key a later
                        // purge of that table would use, and audit rows are never updated.
                        return auditLogRepository.copyToHistory(ids);
                    }

                    @Override
                    public int delete(List<Long> ids) {
                        return auditLogRepository.deleteByIds(ids);
                    }
                });

        if (result.archivedNothing() && !result.failed()) {
            // "Archived 0" and "nothing old enough" are identical in the count alone. AuditLog.createdAt
            // is nullable with no @PrePersist — it is set by convention at each construction site — so
            // a row that missed it is invisible to the cutoff predicate for good. That is the same
            // shape as the NULL updated_at that hid a broken payment_log EOD for months.
            long unarchivable = auditLogRepository.countUnarchivable();
            if (unarchivable > 0) {
                log.error("Audit archival moved nothing and {} audit_log rows have a NULL created_at, "
                                + "so they can never be archived. Backfill them "
                                + "(UPDATE audit_log SET created_at = SYSTIMESTAMP WHERE created_at IS NULL) "
                                + "and find the writer that skipped the stamp.",
                        unarchivable);
            } else {
                log.info("Audit archival moved nothing and no rows have a NULL created_at — "
                        + "audit_log is within its {}-day retention.", retentionDays);
            }
        }
    }
}
