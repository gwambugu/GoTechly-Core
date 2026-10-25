package com.dotechly.gotechly.core.domain;

import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "PENDING_CHANGE")
public class PendingChange implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "ENTITY_TYPE", length = 50, nullable = false)
    private String entityType;

    @NotNull
    @Size(max = 20)
    @Column(name = "CHANGE_TYPE", length = 20, nullable = false)
    private String changeType;

    @Column(name = "ENTITY_ID")
    private Long entityId;

    @NotNull
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "PAYLOAD", nullable = false)
    private String payload;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "PREVIOUS_VALUE")
    private String previousValue;

    @NotNull
    @Size(max = 20)
    @Column(name = "STATUS", length = 20, nullable = false)
    private String status;

    @Size(max = 500)
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @NotNull
    @Size(max = 200)
    @Column(name = "MAKER_USERNAME", length = 200, nullable = false)
    private String makerUsername;

    @NotNull
    @Column(name = "MAKER_DATE", nullable = false)
    private Instant makerDate;

    @Size(max = 200)
    @Column(name = "CHECKER_USERNAME", length = 200)
    private String checkerUsername;

    @Column(name = "CHECKER_DATE")
    private Instant checkerDate;

    @Size(max = 500)
    @Column(name = "CHECKER_REMARKS", length = 500)
    private String checkerRemarks;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMakerUsername() {
        return makerUsername;
    }

    public void setMakerUsername(String makerUsername) {
        this.makerUsername = makerUsername;
    }

    public Instant getMakerDate() {
        return makerDate;
    }

    public void setMakerDate(Instant makerDate) {
        this.makerDate = makerDate;
    }

    public String getCheckerUsername() {
        return checkerUsername;
    }

    public void setCheckerUsername(String checkerUsername) {
        this.checkerUsername = checkerUsername;
    }

    public Instant getCheckerDate() {
        return checkerDate;
    }

    public void setCheckerDate(Instant checkerDate) {
        this.checkerDate = checkerDate;
    }

    public String getCheckerRemarks() {
        return checkerRemarks;
    }

    public void setCheckerRemarks(String checkerRemarks) {
        this.checkerRemarks = checkerRemarks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PendingChange that = (PendingChange) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PendingChange{" +
            "id=" + id +
            ", entityType='" + entityType + '\'' +
            ", changeType='" + changeType + '\'' +
            ", entityId=" + entityId +
            ", status='" + status + '\'' +
            ", makerUsername='" + makerUsername + '\'' +
            ", makerDate=" + makerDate +
            '}';
    }
}
