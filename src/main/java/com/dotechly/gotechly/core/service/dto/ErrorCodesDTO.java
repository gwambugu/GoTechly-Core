package com.dotechly.gotechly.core.service.dto;

import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

public class ErrorCodesDTO implements Serializable {

    private Long id;

    @Size(max = 200)
    private String apiName;

    @Size(max = 200)
    private String serviceName;

    @Size(max = 200)
    private String serviceType;

    @Size(max = 100)
    private String requestType;

    @Size(max = 50)
    private String errorCode;

    @Size(max = 300)
    private String errorDescription;

    @Size(max = 200)
    private String transactionState;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getTransactionState() {
        return transactionState;
    }

    public void setTransactionState(String transactionState) {
        this.transactionState = transactionState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ErrorCodesDTO that = (ErrorCodesDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ErrorCodesDTO{" +
            "id=" + id +
            ", apiName='" + apiName + '\'' +
            ", serviceName='" + serviceName + '\'' +
            ", serviceType='" + serviceType + '\'' +
            ", requestType='" + requestType + '\'' +
            ", errorCode='" + errorCode + '\'' +
            ", errorDescription='" + errorDescription + '\'' +
            ", transactionState='" + transactionState + '\'' +
            '}';
    }
}
