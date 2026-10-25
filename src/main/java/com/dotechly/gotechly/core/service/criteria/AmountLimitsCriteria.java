package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.service.filter.BigDecimalFilter;
import com.dotechly.gotechly.core.service.filter.LongFilter;
import com.dotechly.gotechly.core.service.filter.StringFilter;
import java.io.Serializable;
import java.util.Objects;
public class AmountLimitsCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter channelName;

    private StringFilter serviceName;

    private StringFilter countryCode;

    private StringFilter currencyCode;

    private BigDecimalFilter transactionLimit;

    private BigDecimalFilter dailyLimit;

    private StringFilter status;

    private Boolean distinct;

    public AmountLimitsCriteria() {}

    public AmountLimitsCriteria(AmountLimitsCriteria other) {
        this.id = other.id == null ? null : other.id;
        this.channelName = other.channelName == null ? null : other.channelName;
        this.serviceName = other.serviceName == null ? null : other.serviceName;
        this.countryCode = other.countryCode == null ? null : other.countryCode;
        this.currencyCode = other.currencyCode == null ? null : other.currencyCode;
        this.transactionLimit = other.transactionLimit == null ? null : other.transactionLimit;
        this.dailyLimit = other.dailyLimit == null ? null : other.dailyLimit;
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

    public StringFilter getServiceName() {
        return serviceName;
    }

    public void setServiceName(StringFilter serviceName) {
        this.serviceName = serviceName;
    }

    public StringFilter getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(StringFilter countryCode) {
        this.countryCode = countryCode;
    }

    public StringFilter getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(StringFilter currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimalFilter getTransactionLimit() {
        return transactionLimit;
    }

    public void setTransactionLimit(BigDecimalFilter transactionLimit) {
        this.transactionLimit = transactionLimit;
    }

    public BigDecimalFilter getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimalFilter dailyLimit) {
        this.dailyLimit = dailyLimit;
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
        AmountLimitsCriteria that = (AmountLimitsCriteria) o;
        return
                Objects.equals(id, that.id) &&
                        Objects.equals(channelName, that.channelName) &&
                        Objects.equals(serviceName, that.serviceName) &&
                        Objects.equals(countryCode, that.countryCode) &&
                        Objects.equals(currencyCode, that.currencyCode) &&
                        Objects.equals(transactionLimit, that.transactionLimit) &&
                        Objects.equals(dailyLimit, that.dailyLimit) &&
                        Objects.equals(status, that.status) &&
                        Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, channelName, serviceName, countryCode, currencyCode,
                transactionLimit, dailyLimit, status, distinct);
    }

    @Override
    public String toString() {
        return "AmountLimitsCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (channelName != null ? "channelName=" + channelName + ", " : "") +
                (serviceName != null ? "serviceName=" + serviceName + ", " : "") +
                (countryCode != null ? "countryCode=" + countryCode + ", " : "") +
                (currencyCode != null ? "currencyCode=" + currencyCode + ", " : "") +
                (transactionLimit != null ? "transactionLimit=" + transactionLimit + ", " : "") +
                (dailyLimit != null ? "dailyLimit=" + dailyLimit + ", " : "") +
                (status != null ? "status=" + status + ", " : "") +
                (distinct != null ? "distinct=" + distinct + ", " : "") +
                "}";
    }
}