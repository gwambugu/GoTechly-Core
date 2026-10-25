package com.dotechly.gotechly.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;


@Entity
@Table(name = "amount_limits")
public class AmountLimits implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @Size(max = 200)
    @Column(name = "channel_name", length = 200)
    private String channelName;

    @Size(max = 200)
    @Column(name = "service_name", length = 200)
    private String serviceName;

    @Size(max = 10)
    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Size(max = 10)
    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "transaction_limit", precision = 21, scale = 2)
    private BigDecimal transactionLimit;

    @Column(name = "daily_limit", precision = 21, scale = 2)
    private BigDecimal dailyLimit;

    @Size(max = 50)
    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "date_created")
    private Instant dateCreated;

    @Size(max = 200)
    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "date_modified")
    private Instant dateModified;

    @Size(max = 200)
    @Column(name = "modified_by", length = 200)
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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getTransactionLimit() {
        return transactionLimit;
    }

    public void setTransactionLimit(BigDecimal transactionLimit) {
        this.transactionLimit = transactionLimit;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
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
        AmountLimits that = (AmountLimits) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AmountLimits{" +
                "id=" + getId() +
                ", channelName='" + getChannelName() + "'" +
                ", serviceName='" + getServiceName() + "'" +
                ", countryCode='" + getCountryCode() + "'" +
                ", currencyCode='" + getCurrencyCode() + "'" +
                ", transactionLimit=" + getTransactionLimit() +
                ", dailyLimit=" + getDailyLimit() +
                ", status='" + getStatus() + "'" +
                "}";
    }
}