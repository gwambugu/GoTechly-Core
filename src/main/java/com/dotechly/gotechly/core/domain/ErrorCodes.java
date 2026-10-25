package com.dotechly.gotechly.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "ERROR_CODES")
public class ErrorCodes implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @Size(max = 200)
    @Column(name = "API_NAME", length = 200)
    private String apiName;

    @Size(max = 200)
    @Column(name = "SERVICE_NAME", length = 200)
    private String serviceName;

    @Size(max = 200)
    @Column(name = "SERVICE_TYPE", length = 200)
    private String serviceType;

    @Size(max = 100)
    @Column(name = "REQUEST_TYPE", length = 100)
    private String requestType;

    @Size(max = 50)
    @Column(name = "ERROR_CODE", length = 50)
    private String errorCode;

    @Size(max = 300)
    @Column(name = "ERROR_DESCRIPTION", length = 300)
    private String errorDescription;

    @Size(max = 200)
    @Column(name = "TRANSACTION_STATE", length = 200)
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
        ErrorCodes that = (ErrorCodes) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ErrorCodes{" +
            "id=" + getId() +
            ", apiName='" + getApiName() + "'" +
            ", serviceName='" + getServiceName() + "'" +
            ", serviceType='" + getServiceType() + "'" +
            ", requestType='" + getRequestType() + "'" +
            ", errorCode='" + getErrorCode() + "'" +
            ", errorDescription='" + getErrorDescription() + "'" +
            ", transactionState='" + getTransactionState() + "'" +
            "}";
    }
}
