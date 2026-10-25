package com.dotechly.gotechly.core.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

public class AppConfigsDTO implements Serializable {

    private Long id;

    @Size(max = 200)
    private String configId;

    @Size(max = 3000)
    private String configValue;

    @Size(max = 200)
    private String serviceType;

    @Size(max = 100)
    private String configStatus;

    private byte[] configBinValue;

    /**
     * When true, {@code configValue} is a secret: stored encrypted, and on every
     * read from the portal API this DTO carries the masked placeholder instead of
     * the real value. On UPDATE, a blank/masked {@code configValue} means "keep the
     * existing secret" (see {@code PendingChangeService}).
     */
    @JsonProperty("isSensitive")
    private boolean isSensitive;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getConfigStatus() {
        return configStatus;
    }

    public void setConfigStatus(String configStatus) {
        this.configStatus = configStatus;
    }

    public byte[] getConfigBinValue() {
        return configBinValue;
    }

    public void setConfigBinValue(byte[] configBinValue) {
        this.configBinValue = configBinValue;
    }

    public boolean isSensitive() {
        return isSensitive;
    }

    public void setSensitive(boolean isSensitive) {
        this.isSensitive = isSensitive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AppConfigsDTO that = (AppConfigsDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AppConfigsDTO{" +
            "id=" + id +
            ", configId='" + configId + '\'' +
            '}';
    }
}
