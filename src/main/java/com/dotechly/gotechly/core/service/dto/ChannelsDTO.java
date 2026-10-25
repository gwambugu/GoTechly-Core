package com.dotechly.gotechly.core.service.dto;

import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class ChannelsDTO implements Serializable {

    private Long id;

    @Size(max = 200)
    private String channelName;

    @Size(max = 200)
    private String systemName;

    @Size(max = 200)
    private String apiName;

    @Size(max = 200)
    private String callbackUrl;

    private String callbackRequestTemplate;

    private String authCertificate;

    private String contentType;

    private String expectedResponse;

    @Size(max = 150)
    private String authType;

    private Map<String, String> additionalFields;

    @Size(max = 300)
    private String sourceIps;

    @Size(max = 50)
    private String status;

    private Instant dateCreated;

    @Size(max = 200)
    private String createdBy;

    private Instant dateModified;

    @Size(max = 200)
    private String modifiedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getCallbackRequestTemplate() {
        return callbackRequestTemplate;
    }

    public void setCallbackRequestTemplate(String callbackRequestTemplate) {
        this.callbackRequestTemplate = callbackRequestTemplate;
    }

    public String getAuthCertificate() {
        return authCertificate;
    }

    public void setAuthCertificate(String authCertificate) {
        this.authCertificate = authCertificate;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getExpectedResponse() {
        return expectedResponse;
    }

    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public Map<String, String> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Map<String, String> additionalFields) {
        this.additionalFields = additionalFields;
    }

    public String getSourceIps() {
        return sourceIps;
    }

    public void setSourceIps(String sourceIps) {
        this.sourceIps = sourceIps;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Instant dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getDateModified() {
        return dateModified;
    }

    public void setDateModified(Instant dateModified) {
        this.dateModified = dateModified;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChannelsDTO that = (ChannelsDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ChannelsDTO{" +
            "id=" + id +
            ", channelName='" + channelName + '\'' +
            ", systemName='" + systemName + '\'' +
            ", apiName='" + apiName + '\'' +
            ", authType='" + authType + '\'' +
            '}';
    }
}
