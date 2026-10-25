package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for the AuditLog entity.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    /**
     * Copies the given rows into {@code history_audit_log}.
     *
     * <p><b>The id is carried across.</b> {@code history_audit_log.id} is a NOT NULL primary key with
     * no identity, default or trigger, and this INSERT used to omit the column — so every run with
     * rows to archive raised {@code ORA-01400}, rolled back, and {@code audit_log} was never actually
     * drained despite the job logging a start each night. Ids come from {@code GLOBAL_SEQ} and are
     * unique across both tables, which is why carrying them is safe and why the {@code payment_log}
     * equivalent always did.
     */
    @Modifying
    @Query(value = "INSERT INTO history_audit_log (id, event_type, entity_name, phase_name, old_value, new_value, " +
            "changed_fields, actor_id, actor_type, actor_username, role, ip_address, user_agent, session_id, " +
            "request_id, endpoint, http_method, environment, service_name, created_at) " +
            "SELECT id, event_type, entity_name, phase_name, old_value, new_value, " +
            "changed_fields, actor_id, actor_type, actor_username, role, ip_address, user_agent, session_id, " +
            "request_id, endpoint, http_method, environment, service_name, created_at " +
            "FROM audit_log WHERE id IN (:ids)", nativeQuery = true)
    int copyToHistory(@Param("ids") List<Long> ids);
    /**
     * One page of ids old enough to archive, oldest first.
     *
     * <p>{@code audit_log} is append-only, so age is the whole criterion — unlike
     * {@code deferred_recovery}, whose rows are live work and are selected by state.
     *
     * <p>Ids are selected first and the move keyed on them rather than both statements re-evaluating
     * {@code created_at < cutoff}, so the {@code INSERT} and the {@code DELETE} cannot see different
     * row sets.
     */
    @Query(value = "SELECT id FROM audit_log WHERE created_at < :cutoff "
            + "ORDER BY created_at LIMIT :batchSize", nativeQuery = true)
    List<Long> findArchivableIds(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);

    @Modifying
    @Query(value = "DELETE FROM audit_log WHERE id IN (:ids)", nativeQuery = true)
    int deleteByIds(@Param("ids") List<Long> ids);

    /**
     * Rows that can never be archived because {@code created_at} is NULL.
     *
     * <p>{@code AuditLog.createdAt} is nullable and has no {@code @PrePersist} — it is set by
     * convention at each construction site. If one ever forgets, those rows are invisible to a
     * {@code created_at < cutoff} predicate and accumulate for good, which is precisely how
     * {@code payment_log} grew unbounded while its EOD reported success nightly.
     */
    @Query(value = "SELECT COUNT(*) FROM audit_log WHERE created_at IS NULL", nativeQuery = true)
    long countUnarchivable();
}
