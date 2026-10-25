package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.domain.enumeration.ReqType;
import com.dotechly.gotechly.core.domain.enumeration.ServiceType;
import com.dotechly.gotechly.core.service.filter.InstantFilter;
import com.dotechly.gotechly.core.service.filter.IntegerFilter;
import com.dotechly.gotechly.core.service.filter.LongFilter;
import com.dotechly.gotechly.core.service.filter.StringFilter;
import java.io.Serializable;
import java.util.Objects;

public class EndpointConfigsCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter serviceName;

    private StringFilter serviceType; // Changed to StringFilter as Enums can be filtered by String

    private StringFilter requestType; // Changed to StringFilter as Enums can be filtered by String

    private StringFilter reqTemplate;

    private StringFilter endpoint;

    private StringFilter contentType;

    private StringFilter additionalHeader1;

    private StringFilter additionalHeader2;

    private StringFilter additionalHeader3;

    private StringFilter statusExp;

    private StringFilter tranIDExp;

    private StringFilter errorExp;

    private IntegerFilter timeout;

    private StringFilter timeOutExp;

    private StringFilter providerCode;

    private StringFilter param1;

    private StringFilter param2;

    private StringFilter param3;

    private StringFilter authType;

    private StringFilter modifiedBy;

    private InstantFilter modifyDate;

    private StringFilter hm;

    private Boolean distinct;

    public EndpointConfigsCriteria() {}

    public EndpointConfigsCriteria(EndpointConfigsCriteria other) {
        this.id = other.id == null ? null : other.id;
        this.serviceName = other.serviceName == null ? null : other.serviceName;
        this.serviceType = other.serviceType == null ? null : other.serviceType;
        this.requestType = other.requestType == null ? null : other.requestType;
        this.reqTemplate = other.reqTemplate == null ? null : other.reqTemplate;
        this.endpoint = other.endpoint == null ? null : other.endpoint;
        this.contentType = other.contentType == null ? null : other.contentType;
        this.additionalHeader1 = other.additionalHeader1 == null ? null : other.additionalHeader1;
        this.additionalHeader2 = other.additionalHeader2 == null ? null : other.additionalHeader2;
        this.additionalHeader3 = other.additionalHeader3 == null ? null : other.additionalHeader3;
        this.statusExp = other.statusExp == null ? null : other.statusExp;
        this.tranIDExp = other.tranIDExp == null ? null : other.tranIDExp;
        this.errorExp = other.errorExp == null ? null : other.errorExp;
        this.timeout = other.timeout == null ? null : other.timeout;
        this.timeOutExp = other.timeOutExp == null ? null : other.timeOutExp;
        this.providerCode = other.providerCode == null ? null : other.providerCode;
        this.param1 = other.param1 == null ? null : other.param1;
        this.param2 = other.param2 == null ? null : other.param2;
        this.param3 = other.param3 == null ? null : other.param3;
        this.authType = other.authType == null ? null : other.authType;
        this.modifiedBy = other.modifiedBy == null ? null : other.modifiedBy;
        this.modifyDate = other.modifyDate == null ? null : other.modifyDate;
        this.hm = other.hm == null ? null : other.hm;
        this.distinct = other.distinct;
    }

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
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

    public StringFilter getReqTemplate() {
        return reqTemplate;
    }

    public void setReqTemplate(StringFilter reqTemplate) {
        this.reqTemplate = reqTemplate;
    }

    public StringFilter getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(StringFilter endpoint) {
        this.endpoint = endpoint;
    }

    public StringFilter getContentType() {
        return contentType;
    }

    public void setContentType(StringFilter contentType) {
        this.contentType = contentType;
    }

    public StringFilter getAdditionalHeader1() {
        return additionalHeader1;
    }

    public void setAdditionalHeader1(StringFilter additionalHeader1) {
        this.additionalHeader1 = additionalHeader1;
    }

    public StringFilter getAdditionalHeader2() {
        return additionalHeader2;
    }

    public void setAdditionalHeader2(StringFilter additionalHeader2) {
        this.additionalHeader2 = additionalHeader2;
    }

    public StringFilter getAdditionalHeader3() {
        return additionalHeader3;
    }

    public void setAdditionalHeader3(StringFilter additionalHeader3) {
        this.additionalHeader3 = additionalHeader3;
    }

    public StringFilter getStatusExp() {
        return statusExp;
    }

    public void setStatusExp(StringFilter statusExp) {
        this.statusExp = statusExp;
    }

    public StringFilter getTranIDExp() {
        return tranIDExp;
    }

    public void setTranIDExp(StringFilter tranIDExp) {
        this.tranIDExp = tranIDExp;
    }

    public StringFilter getErrorExp() {
        return errorExp;
    }

    public void setErrorExp(StringFilter errorExp) {
        this.errorExp = errorExp;
    }

    public IntegerFilter getTimeout() {
        return timeout;
    }

    public void setTimeout(IntegerFilter timeout) {
        this.timeout = timeout;
    }

    public StringFilter getTimeOutExp() {
        return timeOutExp;
    }

    public void setTimeOutExp(StringFilter timeOutExp) {
        this.timeOutExp = timeOutExp;
    }

    public StringFilter getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(StringFilter providerCode) {
        this.providerCode = providerCode;
    }

    public StringFilter getParam1() {
        return param1;
    }

    public void setParam1(StringFilter param1) {
        this.param1 = param1;
    }

    public StringFilter getParam2() {
        return param2;
    }

    public void setParam2(StringFilter param2) {
        this.param2 = param2;
    }

    public StringFilter getParam3() {
        return param3;
    }

    public void setParam3(StringFilter param3) {
        this.param3 = param3;
    }

    public StringFilter getAuthType() {
        return authType;
    }

    public void setAuthType(StringFilter authType) {
        this.authType = authType;
    }

    public StringFilter getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(StringFilter modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public InstantFilter getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(InstantFilter modifyDate) {
        this.modifyDate = modifyDate;
    }

    public StringFilter getHm() {
        return hm;
    }

    public void setHm(StringFilter hm) {
        this.hm = hm;
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
        EndpointConfigsCriteria that = (EndpointConfigsCriteria) o;
        return
            Objects.equals(id, that.id) &&
            Objects.equals(serviceName, that.serviceName) &&
            Objects.equals(serviceType, that.serviceType) &&
            Objects.equals(requestType, that.requestType) &&
            Objects.equals(reqTemplate, that.reqTemplate) &&
            Objects.equals(endpoint, that.endpoint) &&
            Objects.equals(contentType, that.contentType) &&
            Objects.equals(additionalHeader1, that.additionalHeader1) &&
            Objects.equals(additionalHeader2, that.additionalHeader2) &&
            Objects.equals(additionalHeader3, that.additionalHeader3) &&
            Objects.equals(statusExp, that.statusExp) &&
            Objects.equals(tranIDExp, that.tranIDExp) &&
            Objects.equals(errorExp, that.errorExp) &&
            Objects.equals(timeout, that.timeout) &&
            Objects.equals(timeOutExp, that.timeOutExp) &&
            Objects.equals(providerCode, that.providerCode) &&
            Objects.equals(param1, that.param1) &&
            Objects.equals(param2, that.param2) &&
            Objects.equals(param3, that.param3) &&
            Objects.equals(authType, that.authType) &&
            Objects.equals(modifiedBy, that.modifiedBy) &&
            Objects.equals(modifyDate, that.modifyDate) &&
            Objects.equals(hm, that.hm) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            serviceName,
            serviceType,
            requestType,
            reqTemplate,
            endpoint,
            contentType,
            additionalHeader1,
            additionalHeader2,
            additionalHeader3,
            statusExp,
            tranIDExp,
            errorExp,
            timeout,
            timeOutExp,
            providerCode,
            param1,
            param2,
            param3,
            authType,
            modifiedBy,
            modifyDate,
            hm,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EndpointConfigsCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (serviceName != null ? "serviceName=" + serviceName + ", " : "") +
            (serviceType != null ? "serviceType=" + serviceType + ", " : "") +
            (requestType != null ? "requestType=" + requestType + ", " : "") +
            (reqTemplate != null ? "reqTemplate=" + reqTemplate + ", " : "") +
            (endpoint != null ? "endpoint=" + endpoint + ", " : "") +
            (contentType != null ? "contentType=" + contentType + ", " : "") +
            (additionalHeader1 != null ? "additionalHeader1=" + additionalHeader1 + ", " : "") +
            (additionalHeader2 != null ? "additionalHeader2=" + additionalHeader2 + ", " : "") +
            (additionalHeader3 != null ? "additionalHeader3=" + additionalHeader3 + ", " : "") +
            (statusExp != null ? "statusExp=" + statusExp + ", " : "") +
            (tranIDExp != null ? "tranIDExp=" + tranIDExp + ", " : "") +
            (errorExp != null ? "errorExp=" + errorExp + ", " : "") +
            (timeout != null ? "timeout=" + timeout + ", " : "") +
            (timeOutExp != null ? "timeOutExp=" + timeOutExp + ", " : "") +
            (providerCode != null ? "providerCode=" + providerCode + ", " : "") +
            (param1 != null ? "param1=" + param1 + ", " : "") +
            (param2 != null ? "param2=" + param2 + ", " : "") +
            (param3 != null ? "param3=" + param3 + ", " : "") +
            (authType != null ? "authType=" + authType + ", " : "") +
            (modifiedBy != null ? "modifiedBy=" + modifiedBy + ", " : "") +
            (modifyDate != null ? "modifyDate=" + modifyDate + ", " : "") +
            (hm != null ? "hm=" + hm + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            "}";
    }
}
