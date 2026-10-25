package com.dotechly.gotechly.core.domain;

import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import com.dotechly.gotechly.core.domain.enumeration.ReqType;
import com.dotechly.gotechly.core.domain.enumeration.ServiceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "ENDPOINT_CONFIGS")
public class EndpointConfigs implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @Size(max = 200)
    @Column(name = "SERVICE_NAME", length = 200)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "SERVICE_TYPE", length = 200)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "REQUEST_TYPE", length = 200)
    private ReqType requestType;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "REQ_TEMPLATE")
    private String reqTemplate;

    @Size(max = 300)
    @Column(name = "ENDPOINT", length = 300)
    private String endpoint;

    @Size(max = 200)
    @Column(name = "CONTENT_TYPE", length = 200)
    private String contentType;

    @Size(max = 200)
    @Column(name = "ADDITIONAL_HEADER_1", length = 200)
    private String additionalHeader1;

    @Size(max = 200)
    @Column(name = "ADDITIONAL_HEADER_2", length = 200)
    private String additionalHeader2;

    @Size(max = 200)
    @Column(name = "ADDITIONAL_HEADER_3", length = 200)
    private String additionalHeader3;

    @Size(max = 500)
    @Column(name = "STATUS_EXP", length = 500)
    private String statusExp;

    @Size(max = 500)
    @Column(name = "TRAN_ID_EXP", length = 500)
    private String tranIDExp;

    @Size(max = 500)
    @Column(name = "ERROR_EXP", length = 500)
    private String errorExp;

    @Column(name = "TIMEOUT")
    private Integer timeout;

    @Size(max = 500)
    @Column(name = "TIME_OUT_EXP", length = 500)
    private String timeOutExp;

    @Size(max = 300)
    @Column(name = "PROVIDER_CODE", length = 300)
    private String providerCode;

    @Size(max = 200)
    @Column(name = "PARAM_1", length = 200)
    private String param1;

    @Size(max = 200)
    @Column(name = "PARAM_2", length = 200)
    private String param2;

    @Size(max = 200)
    @Column(name = "PARAM_3", length = 200)
    private String param3;

    @Size(max = 150)
    @Column(name = "AUTH_TYPE", length = 150)
    private String authType;

    @Size(max = 200)
    @Column(name = "MODIFIED_BY", length = 200)
    private String modifiedBy;

    @Column(name = "MODIFY_DATE")
    private Instant modifyDate;

    @Size(max = 200)
    @Column(name = "HM", length = 200)
    private String hm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public ReqType getRequestType() {
        return requestType;
    }

    public void setRequestType(ReqType requestType) {
        this.requestType = requestType;
    }

    public String getReqTemplate() {
        return reqTemplate;
    }

    public void setReqTemplate(String reqTemplate) {
        this.reqTemplate = reqTemplate;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getAdditionalHeader1() {
        return additionalHeader1;
    }

    public void setAdditionalHeader1(String additionalHeader1) {
        this.additionalHeader1 = additionalHeader1;
    }

    public String getAdditionalHeader2() {
        return additionalHeader2;
    }

    public void setAdditionalHeader2(String additionalHeader2) {
        this.additionalHeader2 = additionalHeader2;
    }

    public String getAdditionalHeader3() {
        return additionalHeader3;
    }

    public void setAdditionalHeader3(String additionalHeader3) {
        this.additionalHeader3 = additionalHeader3;
    }

    public String getStatusExp() {
        return statusExp;
    }

    public void setStatusExp(String statusExp) {
        this.statusExp = statusExp;
    }

    public String getTranIDExp() {
        return tranIDExp;
    }

    public void setTranIDExp(String tranIDExp) {
        this.tranIDExp = tranIDExp;
    }

    public String getErrorExp() {
        return errorExp;
    }

    public void setErrorExp(String errorExp) {
        this.errorExp = errorExp;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getTimeOutExp() {
        return timeOutExp;
    }

    public void setTimeOutExp(String timeOutExp) {
        this.timeOutExp = timeOutExp;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getParam1() {
        return param1;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
    }

    public String getParam2() {
        return param2;
    }

    public void setParam2(String param2) {
        this.param2 = param2;
    }

    public String getParam3() {
        return param3;
    }

    public void setParam3(String param3) {
        this.param3 = param3;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Instant getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Instant modifyDate) {
        this.modifyDate = modifyDate;
    }

    public String getHm() {
        return hm;
    }

    public void setHm(String hm) {
        this.hm = hm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndpointConfigs that = (EndpointConfigs) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EndpointConfigs{" +
            "id=" + getId() +
            ", serviceName='" + getServiceName() + "'" +
            ", serviceType='" + getServiceType() + "'" +
            ", requestType='" + getRequestType() + "'" +

            ", endpoint='" + getEndpoint() + "'" +
            ", contentType='" + getContentType() + "'" +
            ", additionalHeader1='" + getAdditionalHeader1() + "'" +
            ", additionalHeader2='" + getAdditionalHeader2() + "'" +
            ", additionalHeader3='" + getAdditionalHeader3() + "'" +
            ", statusExp='" + getStatusExp() + "'" +
            ", tranIDExp='" + getTranIDExp() + "'" +
            ", errorExp='" + getErrorExp() + "'" +
            ", timeout=" + getTimeout() +
            ", timeOutExp='" + getTimeOutExp() + "'" +
            ", providerCode='" + getProviderCode() + "'" +
            ", param1='" + getParam1() + "'" +
            ", param2='" + getParam2() + "'" +
            ", param3='" + getParam3() + "'" +
            ", authType='" + getAuthType() + "'" +
            ", modifiedBy='" + getModifiedBy() + "'" +
            ", modifyDate='" + getModifyDate() + "'" +
            ", hm='" + getHm() + "'" +
            "}";
    }
}
