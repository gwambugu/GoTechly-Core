package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.service.filter.LongFilter;
import com.dotechly.gotechly.core.service.filter.StringFilter;
import java.io.Serializable;
import java.util.Objects;

public class ErrorCodesCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter apiName;

    private StringFilter serviceName;

    private StringFilter serviceType;

    private StringFilter requestType;

    private StringFilter errorCode;

    private StringFilter errorDescription;

    private StringFilter transactionState;

    private Boolean distinct;

    public ErrorCodesCriteria() {}

    public ErrorCodesCriteria(ErrorCodesCriteria other) {
        this.id = other.id == null ? null : other.id;
        this.apiName = other.apiName == null ? null : other.apiName;
        this.serviceName = other.serviceName == null ? null : other.serviceName;
        this.serviceType = other.serviceType == null ? null : other.serviceType;
        this.requestType = other.requestType == null ? null : other.requestType;
        this.errorCode = other.errorCode == null ? null : other.errorCode;
        this.errorDescription = other.errorDescription == null ? null : other.errorDescription;
        this.transactionState = other.transactionState == null ? null : other.transactionState;
        this.distinct = other.distinct;
    }

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getApiName() {
        return apiName;
    }

    public void setApiName(StringFilter apiName) {
        this.apiName = apiName;
    }

    public StringFilter getServiceName() {
        return serviceName;
    }

    public void setServiceName(StringFilter serviceName) {
        this.serviceName = serviceName;
    }

    public StringFilter getServiceType() {
        return serviceType;
    }

    public void setServiceType(StringFilter serviceType) {
        this.serviceType = serviceType;
    }

    public StringFilter getRequestType() {
        return requestType;
    }

    public void setRequestType(StringFilter requestType) {
        this.requestType = requestType;
    }

    public StringFilter getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(StringFilter errorCode) {
        this.errorCode = errorCode;
    }

    public StringFilter getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(StringFilter errorDescription) {
        this.errorDescription = errorDescription;
    }

    public StringFilter getTransactionState() {
        return transactionState;
    }

    public void setTransactionState(StringFilter transactionState) {
        this.transactionState = transactionState;
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
        ErrorCodesCriteria that = (ErrorCodesCriteria) o;
        return
            Objects.equals(id, that.id) &&
            Objects.equals(apiName, that.apiName) &&
            Objects.equals(serviceName, that.serviceName) &&
            Objects.equals(serviceType, that.serviceType) &&
            Objects.equals(requestType, that.requestType) &&
            Objects.equals(errorCode, that.errorCode) &&
            Objects.equals(errorDescription, that.errorDescription) &&
            Objects.equals(transactionState, that.transactionState) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            apiName,
            serviceName,
            serviceType,
            requestType,
            errorCode,
            errorDescription,
            transactionState,
            distinct
        );
    }

    @Override
    public String toString() {
        return "ErrorCodesCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (apiName != null ? "apiName=" + apiName + ", " : "") +
            (serviceName != null ? "serviceName=" + serviceName + ", " : "") +
            (serviceType != null ? "serviceType=" + serviceType + ", " : "") +
            (requestType != null ? "requestType=" + requestType + ", " : "") +
            (errorCode != null ? "errorCode=" + errorCode + ", " : "") +
            (errorDescription != null ? "errorDescription=" + errorDescription + ", " : "") +
            (transactionState != null ? "transactionState=" + transactionState + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            "}";
    }
}
