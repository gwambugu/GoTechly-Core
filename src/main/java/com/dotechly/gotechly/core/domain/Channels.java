package com.dotechly.gotechly.core.domain;

import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "CHANNELS")
public class Channels implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @Size(max = 200)
    @Column(name = "CHANNEL_NAME", length = 200, unique = true)
    private String channelName;

    @Size(max = 200)
    @Column(name = "SYSTEM_NAME", length = 200)
    private String systemName;

    @Size(max = 200)
    @Column(name = "API_NAME", length = 200)
    private String apiName;

    @Size(max = 200)
    @Column(name = "CALLBACK_URL", length = 200)
    private String callbackUrl;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "CALLBACK_REQUEST_TEMPLATE")
    private String callbackRequestTemplate;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "AUTH_CERTIFICATE")
    private String authCertificate;

    @Column(name = "CONTENT_TYPE")
    private String contentType;

    @Column(name = "EXPECTED_RESPONSE")
    private String expectedResponse;

    @Size(max = 150)
    @Column(name = "AUTH_TYPE", length = 150)
    private String authType;

    @Convert(converter = JsonMapConverter.class)
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "ADDITIONAL_FIELDS")
    private Map<String, String> additionalFields;

    @Size(max = 300)
    @Column(name = "SOURCE_IPS", length = 300)
    private String sourceIps;

    @Size(max = 50)
    @Column(name = "STATUS", length = 50)
    private String status;

    @Column(name = "DATE_CREATED")
    private Instant dateCreated;

    @Size(max = 200)
    @Column(name = "CREATED_BY", length = 200)
    private String createdBy;

    @Column(name = "DATE_MODIFIED")
    private Instant dateModified;

    @Size(max = 200)
    @Column(name = "MODIFIED_BY", length = 200)
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

    public void addAdditionalField(String key, String value) {
        if (this.additionalFields == null) this.additionalFields = new HashMap<>();
        this.additionalFields.put(key, value);
    }

    public void removeAdditionalField(String key) {
        if (this.additionalFields != null) this.additionalFields.remove(key);
    }

    public String getAdditionalField(String key) {
        return this.additionalFields != null ? this.additionalFields.get(key) : null;
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
        Channels that = (Channels) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Channels{" +
            "id=" + getId() +
            ", channelName='" + getChannelName() + "'" +
            ", systemName='" + getSystemName() + "'" +
            ", apiName='" + getApiName() + "'" +
            ", authType='" + getAuthType() + "'" +
            ", contentType='" + getContentType() + "'" +
            "}";
    }
}
