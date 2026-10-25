package com.dotechly.gotechly.core.domain;

import com.dotechly.gotechly.core.recovery.RecoveryModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;

/**
 * One transaction whose core-banking state has to be brought into line with the answer the scheme
 * already holds.
 *
 * <p><b>This row is the source of truth, not the in-memory queue.</b> The queue is a working set: a
 * restart, a deploy or a crash empties it, and what it would be dropping is precisely the population
 * where money may sit posted in Finacle while the scheme has been told the transfer failed. Losing
 * that silently is unrecoverable and invisible; a row is neither. The row is written first and the
 * queue offered second, which is also why a full queue can reject nothing — the sweeper picks up
 * whatever the queue could not hold.
 *
 * <p>Kept deliberately separate from {@code payment_log}: different lifecycle, different retention
 * (the EOD job archives payment_log), and "what is stuck right now" stays a cheap indexed query
 * instead of a scan over every transaction ever taken.
 */
@Entity
@Table(name = "deferred_recovery")
public class DeferredRecovery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    /**
     * The transaction being reconciled. Unique: a second enqueue for the same transaction is the same
     * piece of work, and two rows would mean two instances reversing the same posting.
     */
    @NotNull
    @Size(max = 70)
    @Column(name = "request_id", length = 70, nullable = false, unique = true)
    private String requestId;

    @NotNull
    @Size(max = 50)
    @Column(name = "payment_type", length = 50, nullable = false)
    private String paymentType;

    @NotNull
    @Size(max = 20)
    @Column(name = "direction", length = 20, nullable = false)
    private String direction;

    /** What the scheme was told. Written once at enqueue and never updated. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "declared_outcome", length = 20, nullable = false)
    private RecoveryModel.DeclaredOutcome declaredOutcome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 24, nullable = false)
    private RecoveryModel.RecoveryState state;

    /** What the gate last reported. Null until the first attempt runs. */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_cbs_state", length = 20)
    private RecoveryModel.CbsState lastCbsState;

    /** The action the last attempt chose, for the operator reading the row. */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_action", length = 24)
    private RecoveryModel.Action lastAction;

    /** The halt code that put this transaction into recovery in the first place. */
    @Size(max = 100)
    @Column(name = "trigger_halt_code", length = 100)
    private String triggerHaltCode;

    /** Phase the last repair ran, e.g. {@code recovery-reverse}. */
    @Size(max = 100)
    @Column(name = "last_phase", length = 100)
    private String lastPhase;

    @Size(max = 500)
    @Column(name = "last_detail", length = 500)
    private String lastDetail;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    /** When this becomes eligible again. The backoff lives here, not in a sleep. */
    @NotNull
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    /**
     * Hard stop. Past this the row goes to {@code NEEDS_ATTENTION} whatever the attempt count says —
     * a core-banking outage lasting hours would otherwise burn the attempt budget in minutes and
     * escalate work that only needed patience.
     */
    @NotNull
    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    /** Which instance currently holds the lease. Null when unclaimed. */
    @Size(max = 100)
    @Column(name = "owner_instance", length = 100)
    private String ownerInstance;

    /** When the lease was taken. A lease older than the configured duration is up for grabs. */
    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Guards the claim. Two instances reading the same unclaimed row both write an owner; the loser's
     * flush fails rather than both of them reversing the same posting.
     */
    @Version
    @Column(name = "row_version")
    private Long rowVersion;

    @PrePersist
    void onInsert() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** A row nobody needs to work again. */
    public boolean isTerminal() {
        return state == RecoveryModel.RecoveryState.RECONCILED
                || state == RecoveryModel.RecoveryState.NEEDS_ATTENTION
                || state == RecoveryModel.RecoveryState.CANCELLED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public RecoveryModel.DeclaredOutcome getDeclaredOutcome() { return declaredOutcome; }
    public void setDeclaredOutcome(RecoveryModel.DeclaredOutcome declaredOutcome) { this.declaredOutcome = declaredOutcome; }

    public RecoveryModel.RecoveryState getState() { return state; }
    public void setState(RecoveryModel.RecoveryState state) { this.state = state; }

    public RecoveryModel.CbsState getLastCbsState() { return lastCbsState; }
    public void setLastCbsState(RecoveryModel.CbsState lastCbsState) { this.lastCbsState = lastCbsState; }

    public RecoveryModel.Action getLastAction() { return lastAction; }
    public void setLastAction(RecoveryModel.Action lastAction) { this.lastAction = lastAction; }

    public String getTriggerHaltCode() { return triggerHaltCode; }
    public void setTriggerHaltCode(String triggerHaltCode) { this.triggerHaltCode = triggerHaltCode; }

    public String getLastPhase() { return lastPhase; }
    public void setLastPhase(String lastPhase) { this.lastPhase = lastPhase; }

    public String getLastDetail() { return lastDetail; }
    public void setLastDetail(String lastDetail) { this.lastDetail = lastDetail; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }

    public String getOwnerInstance() { return ownerInstance; }
    public void setOwnerInstance(String ownerInstance) { this.ownerInstance = ownerInstance; }

    public Instant getClaimedAt() { return claimedAt; }
    public void setClaimedAt(Instant claimedAt) { this.claimedAt = claimedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getRowVersion() { return rowVersion; }
    public void setRowVersion(Long rowVersion) { this.rowVersion = rowVersion; }
}
