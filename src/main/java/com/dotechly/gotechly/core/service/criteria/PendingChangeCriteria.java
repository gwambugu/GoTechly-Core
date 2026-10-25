package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.service.filter.LongFilter;
import com.dotechly.gotechly.core.service.filter.StringFilter;

import java.io.Serializable;
import java.util.Objects;

public class PendingChangeCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;
    private StringFilter entityType;
    private StringFilter changeType;
    private StringFilter status;
    private StringFilter makerUsername;
    private StringFilter checkerUsername;
    private Boolean distinct;

    public PendingChangeCriteria() {}

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getEntityType() {
        return entityType;
    }

    public void setEntityType(StringFilter entityType) {
        this.entityType = entityType;
    }

    public StringFilter getChangeType() {
        return changeType;
    }

    public void setChangeType(StringFilter changeType) {
        this.changeType = changeType;
    }

    public StringFilter getStatus() {
        return status;
    }

    public void setStatus(StringFilter status) {
        this.status = status;
    }

    public StringFilter getMakerUsername() {
        return makerUsername;
    }

    public void setMakerUsername(StringFilter makerUsername) {
        this.makerUsername = makerUsername;
    }

    public StringFilter getCheckerUsername() {
        return checkerUsername;
    }

    public void setCheckerUsername(StringFilter checkerUsername) {
        this.checkerUsername = checkerUsername;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PendingChangeCriteria that = (PendingChangeCriteria) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(entityType, that.entityType) &&
            Objects.equals(changeType, that.changeType) &&
            Objects.equals(status, that.status) &&
            Objects.equals(makerUsername, that.makerUsername) &&
            Objects.equals(checkerUsername, that.checkerUsername) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, entityType, changeType, status, makerUsername, checkerUsername, distinct);
    }

    @Override
    public String toString() {
        return "PendingChangeCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (entityType != null ? "entityType=" + entityType + ", " : "") +
            (changeType != null ? "changeType=" + changeType + ", " : "") +
            (status != null ? "status=" + status + ", " : "") +
            (makerUsername != null ? "makerUsername=" + makerUsername + ", " : "") +
            (checkerUsername != null ? "checkerUsername=" + checkerUsername + ", " : "") +
            "}";
    }
}
