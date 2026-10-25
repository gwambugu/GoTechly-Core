package com.dotechly.gotechly.core.domain;

import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "app_configs")
public class AppConfigs implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @Size(max = 200)
    @Column(name = "config_id", length = 200)
    private String configId;

    @Size(max = 3000)
    @Column(name = "config_value", length = 3000)
    private String configValue;

    @Size(max = 200)
    @Column(name = "service_type", length = 200)
    private String serviceType;

    @Size(max = 100)
    @Column(name = "config_status", length = 100)
    private String configStatus;

    @JdbcTypeCode(SqlTypes.LONG32VARBINARY)
    @Column(name = "config_bin_value")
    private byte[] configBinValue;

    /**
     * When true, {@code config_value} holds a secret and is stored encrypted at rest,
     * decrypted only on the plugin/SPI read path, and masked on every portal response.
     */
    @Column(name = "is_sensitive", nullable = false)
    private boolean isSensitive = false;

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
        AppConfigs that = (AppConfigs) o;
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
        return "AppConfigs{" +
            "id=" + getId() +
            ", configId='" + getConfigId() + "'" +
            ", configValue='" + getConfigValue() + "'" +
            ", serviceType='" + getServiceType() + "'" +
            ", configStatus='" + getConfigStatus() + "'" +
            ", configBinValue='" + getConfigBinValue() + "'" +
            "}";
    }
}
