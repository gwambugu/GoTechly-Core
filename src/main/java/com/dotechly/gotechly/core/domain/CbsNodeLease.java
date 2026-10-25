package com.dotechly.gotechly.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;

/**
 * Which Core instance owns which decimal block of the DE11 (STAN) space.
 *
 * <p>Ten fixed rows, seeded by Liquibase and never inserted or deleted at runtime — an instance only
 * ever takes or gives up ownership of a row that already exists. That is what makes claiming a plain
 * conditional {@code UPDATE} with no INSERT race to lose.
 *
 * <p>See {@code StanGenerator} for what the ordinal buys and {@code StanNodeLeaseService} for the
 * claim/heartbeat/release cycle.
 */
@Entity
@Table(name = "cbs_node_lease")
public class CbsNodeLease implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 0..9. The ordinal is the key; there is nothing else to identify a block by. */
    @Id
    @Column(name = "node_id", nullable = false)
    private Integer nodeId;

    /**
     * {@code InstanceInfo.getInstanceId()} — {@code hostname:server.port}. Null means free.
     *
     * <p>Not unique: a stale takeover can move an instance to a different ordinal, and the claim
     * clears the instance's previous row in the same transaction rather than colliding with a
     * constraint on the way.
     */
    @Column(name = "owner", length = 200)
    private String owner;

    /** When this owner took the block. */
    @Column(name = "leased_at")
    private Instant leasedAt;

    /**
     * Last proof of life from the owner. Distinct from {@link #leasedAt}: an instance that has held
     * an ordinal for a week is normal, one that last checked in an hour ago is not.
     */
    @Column(name = "heartbeat_at")
    private Instant heartbeatAt;

    public Integer getNodeId() { return nodeId; }

    public void setNodeId(Integer nodeId) { this.nodeId = nodeId; }

    public String getOwner() { return owner; }

    public void setOwner(String owner) { this.owner = owner; }

    public Instant getLeasedAt() { return leasedAt; }

    public void setLeasedAt(Instant leasedAt) { this.leasedAt = leasedAt; }

    public Instant getHeartbeatAt() { return heartbeatAt; }

    public void setHeartbeatAt(Instant heartbeatAt) { this.heartbeatAt = heartbeatAt; }
}
