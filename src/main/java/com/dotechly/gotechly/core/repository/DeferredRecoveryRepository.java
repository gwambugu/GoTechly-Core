package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.DeferredRecovery;
import com.dotechly.gotechly.core.recovery.RecoveryModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeferredRecoveryRepository extends JpaRepository<DeferredRecovery, Long> {

    Optional<DeferredRecovery> findByRequestId(String requestId);

    Page<DeferredRecovery> findByState(RecoveryModel.RecoveryState state, Pageable pageable);

    long countByState(RecoveryModel.RecoveryState state);

    /**
     * Rows the sweeper should pull back into the working set: due, and either unclaimed or holding a
     * lease that has expired.
     *
     * <p>The expired-lease arm is what makes a crash survivable. An instance that dies mid-attempt
     * leaves its rows {@code IN_PROGRESS} with its own name on them, and without this they would sit
     * there forever — the one outcome worse than retrying, because nothing is even trying.
     */
    @Query("""
            select r from DeferredRecovery r
             where r.nextAttemptAt <= :now
               and (r.state = com.dotechly.gotechly.core.recovery.RecoveryModel$RecoveryState.PENDING
                    or (r.state = com.dotechly.gotechly.core.recovery.RecoveryModel$RecoveryState.IN_PROGRESS
                        and (r.claimedAt is null or r.claimedAt < :leaseCutoff)))
             order by r.nextAttemptAt asc
            """)
    List<DeferredRecovery> findClaimable(@Param("now") Instant now,
                                         @Param("leaseCutoff") Instant leaseCutoff,
                                         Pageable pageable);

    /** Live rows past their hard deadline, so the sweeper can escalate them without a worker. */
    @Query("""
            select r from DeferredRecovery r
             where r.deadlineAt <= :now
               and r.state in (com.dotechly.gotechly.core.recovery.RecoveryModel$RecoveryState.PENDING,
                               com.dotechly.gotechly.core.recovery.RecoveryModel$RecoveryState.IN_PROGRESS)
            """)
    List<DeferredRecovery> findExpired(@Param("now") Instant now, Pageable pageable);

    // ------------------------------------------------------------------ archival
    //
    // Native SQL rather than JPA: this moves rows between two tables without loading any of them,
    // which is the only shape that stays cheap as the table grows.

    /**
     * One page of ids eligible for archival, oldest first.
     *
     * <p>Ids are selected first and the move is then keyed on them, rather than both statements
     * re-evaluating {@code updated_at < cutoff}. The predicate reads a column the reconciler writes
     * on every attempt, so re-evaluating it means the {@code INSERT} and the {@code DELETE} can see
     * different row sets if anything changes between them — and the difference would be rows deleted
     * without being archived.
     *
     * <p><b>State is the real filter, not age.</b> {@code PENDING} and {@code IN_PROGRESS} are live
     * work whatever their age — a row pending for a fortnight is a corridor that has been down for a
     * fortnight, not a stale record. {@code NEEDS_ATTENTION} is excluded for a different reason: it
     * is an open money position whose only visibility is the count on the recovery metrics endpoint,
     * so archiving it would drop that count to zero and make an unresolved position look settled.
     */
    @Query(value = "SELECT id FROM deferred_recovery "
            + "WHERE state IN ('RECONCILED', 'CANCELLED') AND updated_at < :cutoff "
            + "ORDER BY updated_at LIMIT :batchSize", nativeQuery = true)
    List<Long> findArchivableIds(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);

    /**
     * Copies the given rows into {@code history_deferred_recovery}.
     *
     * <p>The id is carried across, not regenerated: it comes from {@code GLOBAL_SEQ} so it is unique
     * across both tables, and keeping it means an id quoted in a log line or a support ticket still
     * resolves after archival.
     */
    @Modifying
    @Query(value = "INSERT INTO history_deferred_recovery (id, request_id, payment_type, direction, "
            + "declared_outcome, state, last_cbs_state, last_action, trigger_halt_code, last_phase, "
            + "last_detail, attempts, next_attempt_at, deadline_at, owner_instance, claimed_at, "
            + "created_at, updated_at, row_version, archived_at) "
            + "SELECT id, request_id, payment_type, direction, "
            + "declared_outcome, state, last_cbs_state, last_action, trigger_halt_code, last_phase, "
            + "last_detail, attempts, next_attempt_at, deadline_at, owner_instance, claimed_at, "
            + "created_at, updated_at, row_version, :archivedAt "
            + "FROM deferred_recovery WHERE id IN (:ids)", nativeQuery = true)
    int copyToHistory(@Param("ids") List<Long> ids, @Param("archivedAt") Instant archivedAt);

    @Modifying
    @Query(value = "DELETE FROM deferred_recovery WHERE id IN (:ids)", nativeQuery = true)
    int deleteByIds(@Param("ids") List<Long> ids);

    /**
     * Rows that will never be archived because they are still open. Reported when a run archives
     * nothing, so "archived 0" can be told apart from "nothing left to archive" — the distinction
     * that hid a broken EOD on {@code payment_log} for months.
     */
    @Query(value = "SELECT COUNT(*) FROM deferred_recovery WHERE state NOT IN ('RECONCILED', 'CANCELLED')",
            nativeQuery = true)
    long countUnarchivable();
}
