package com.dotechly.gotechly.core.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class PendingChangeDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 50)
    private String entityType;

    @NotNull
    @Size(max = 20)
    private String changeType;

    private Long entityId;

    @NotNull
    private String payload;

    private String previousValue;

    @Size(max = 20)
    private String status;

    @Size(max = 500)
    private String description;

    @Size(max = 200)
    private String makerUsername;

    private Instant makerDate;

    @Size(max = 200)
    private String checkerUsername;

    private Instant checkerDate;

    @Size(max = 500)
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
        PendingChangeDTO that = (PendingChangeDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PendingChangeDTO{" +
            "id=" + id +
            ", entityType='" + entityType + '\'' +
            ", changeType='" + changeType + '\'' +
            ", entityId=" + entityId +
            ", status='" + status + '\'' +
            ", makerUsername='" + makerUsername + '\'' +
            '}';
    }
}
