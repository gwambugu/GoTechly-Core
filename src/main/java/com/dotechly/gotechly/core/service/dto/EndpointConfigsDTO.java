package com.dotechly.gotechly.core.service.dto;

import com.dotechly.gotechly.core.domain.enumeration.ReqType;
import com.dotechly.gotechly.core.domain.enumeration.ServiceType;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class EndpointConfigsDTO implements Serializable {

    private Long id;

    @Size(max = 200)
    private String serviceName;

    private ServiceType serviceType;

    private ReqType requestType;

    private String reqTemplate;

    @Size(max = 300)
    private String endpoint;

    @Size(max = 200)
    private String contentType;

    @Size(max = 200)
    private String additionalHeader1;

    @Size(max = 200)
    private String additionalHeader2;

    @Size(max = 200)
    private String additionalHeader3;

    @Size(max = 500)
    private String statusExp;

    @Size(max = 500)
    private String tranIDExp;

    @Size(max = 500)
    private String errorExp;

    private Integer timeout;

    @Size(max = 500)
    private String timeOutExp;

    @Size(max = 300)
    private String providerCode;

    @Size(max = 200)
    private String param1;

    @Size(max = 200)
    private String param2;

    @Size(max = 200)
    private String param3;

    @Size(max = 150)
    private String authType;

    @Size(max = 200)
    private String modifiedBy;

    private Instant modifyDate;

    @Size(max = 200)
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
        EndpointConfigsDTO that = (EndpointConfigsDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EndpointConfigsDTO{" +
            "id=" + id +
            ", serviceName='" + serviceName + '\'' +
            ", authType='" + authType + '\'' +
            '}';
    }
}
