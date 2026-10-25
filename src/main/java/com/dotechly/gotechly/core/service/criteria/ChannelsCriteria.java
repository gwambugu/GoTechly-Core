package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.service.filter.LongFilter;
import com.dotechly.gotechly.core.service.filter.StringFilter;
import java.io.Serializable;
import java.util.Objects;

public class ChannelsCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter channelName;

    private StringFilter systemName;

    private StringFilter apiName;

    private StringFilter callbackUrl;

    private StringFilter contentType;

    private StringFilter authType;

    private StringFilter sourceIps;

    private StringFilter status;

    private Boolean distinct;

    public ChannelsCriteria() {}

    public ChannelsCriteria(ChannelsCriteria other) {
        this.id = other.id == null ? null : other.id;
        this.channelName = other.channelName == null ? null : other.channelName;
        this.systemName = other.systemName == null ? null : other.systemName;
        this.apiName = other.apiName == null ? null : other.apiName;
        this.callbackUrl = other.callbackUrl == null ? null : other.callbackUrl;
        this.contentType = other.contentType == null ? null : other.contentType;
        this.authType = other.authType == null ? null : other.authType;
        this.sourceIps = other.sourceIps == null ? null : other.sourceIps;
        this.status = other.status == null ? null : other.status;
        this.distinct = other.distinct;
    }

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getChannelName() {
        return channelName;
    }

    public void setChannelName(StringFilter channelName) {
        this.channelName = channelName;
    }

    public StringFilter getSystemName() {
        return systemName;
    }

    public void setSystemName(StringFilter systemName) {
        this.systemName = systemName;
    }

    public StringFilter getApiName() {
        return apiName;
    }

    public void setApiName(StringFilter apiName) {
        this.apiName = apiName;
    }

    public StringFilter getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(StringFilter callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public StringFilter getContentType() {
        return contentType;
    }

    public void setContentType(StringFilter contentType) {
        this.contentType = contentType;
    }

    public StringFilter getAuthType() {
        return authType;
    }

    public void setAuthType(StringFilter authType) {
        this.authType = authType;
    }

    public StringFilter getSourceIps() {
        return sourceIps;
    }

    public void setSourceIps(StringFilter sourceIps) {
        this.sourceIps = sourceIps;
    }

    public StringFilter getStatus() {
        return status;
    }

    public void setStatus(StringFilter status) {
        this.status = status;
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
        ChannelsCriteria that = (ChannelsCriteria) o;
        return
            Objects.equals(id, that.id) &&
            Objects.equals(channelName, that.channelName) &&
            Objects.equals(systemName, that.systemName) &&
            Objects.equals(apiName, that.apiName) &&
            Objects.equals(callbackUrl, that.callbackUrl) &&
            Objects.equals(contentType, that.contentType) &&
            Objects.equals(authType, that.authType) &&
            Objects.equals(sourceIps, that.sourceIps) &&
            Objects.equals(status, that.status) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, channelName, systemName, apiName, callbackUrl, contentType, authType, sourceIps, status, distinct);
    }

    @Override
    public String toString() {
        return "ChannelsCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (channelName != null ? "channelName=" + channelName + ", " : "") +
            (systemName != null ? "systemName=" + systemName + ", " : "") +
            (apiName != null ? "apiName=" + apiName + ", " : "") +
            (callbackUrl != null ? "callbackUrl=" + callbackUrl + ", " : "") +
            (contentType != null ? "contentType=" + contentType + ", " : "") +
            (authType != null ? "authType=" + authType + ", " : "") +
            (sourceIps != null ? "sourceIps=" + sourceIps + ", " : "") +
            (status != null ? "status=" + status + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            "}";
    }
}
