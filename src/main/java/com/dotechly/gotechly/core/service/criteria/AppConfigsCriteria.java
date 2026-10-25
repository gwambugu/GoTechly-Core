package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.service.filter.LongFilter;
import com.dotechly.gotechly.core.service.filter.StringFilter;
import java.io.Serializable;
import java.util.Objects;

public class AppConfigsCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter configId;

    private StringFilter configValue;

    private StringFilter serviceType;

    private StringFilter configStatus;

    private Boolean distinct;

    public AppConfigsCriteria() {}

    public AppConfigsCriteria(AppConfigsCriteria other) {
        this.id = other.id == null ? null : other.id;
        this.configId = other.configId == null ? null : other.configId;
        this.configValue = other.configValue == null ? null : other.configValue;
        this.serviceType = other.serviceType == null ? null : other.serviceType;
        this.configStatus = other.configStatus == null ? null : other.configStatus;
        this.distinct = other.distinct;
    }

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getConfigId() {
        return configId;
    }

    public void setConfigId(StringFilter configId) {
        this.configId = configId;
    }

    public StringFilter getConfigValue() {
        return configValue;
    }

    public void setConfigValue(StringFilter configValue) {
        this.configValue = configValue;
    }

    public StringFilter getServiceType() {
        return serviceType;
    }

    public void setServiceType(StringFilter serviceType) {
        this.serviceType = serviceType;
    }

    public StringFilter getConfigStatus() {
        return configStatus;
    }

    public void setConfigStatus(StringFilter configStatus) {
        this.configStatus = configStatus;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AppConfigsCriteria that = (AppConfigsCriteria) o;
        return
            Objects.equals(id, that.id) &&
            Objects.equals(configId, that.configId) &&
            Objects.equals(configValue, that.configValue) &&
            Objects.equals(serviceType, that.serviceType) &&
            Objects.equals(configStatus, that.configStatus) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            configId,
            configValue,
            serviceType,
            configStatus,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AppConfigsCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (configId != null ? "configId=" + configId + ", " : "") +
            (configValue != null ? "configValue=" + configValue + ", " : "") +
            (serviceType != null ? "serviceType=" + serviceType + ", " : "") +
            (configStatus != null ? "configStatus=" + configStatus + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            "}";
    }
}
