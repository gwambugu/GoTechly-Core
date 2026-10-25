package com.dotechly.gotechly.core.service.criteria;


import com.dotechly.gotechly.core.service.filter.IntegerFilter;
import com.dotechly.gotechly.core.service.filter.LongFilter;
import com.dotechly.gotechly.core.service.filter.StringFilter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria for querying {@link com.dotechly.gotechly.core.domain.LinkedWalletSubscription}.
 * Exposes the commonly-searched lookup columns; not every column needs a filter.
 */
public class LinkedWalletSubscriptionCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;
    private StringFilter paymentType;
    private StringFilter aliasName;
    private StringFilter requestId;
    private StringFilter serviceType;
    private StringFilter pspName;
    private StringFilter mobilePhoneNumber;
    private StringFilter savingsAccountNumber;
    private StringFilter bankName;
    private StringFilter bankShortCode;
    private StringFilter referenceNumber;
    private StringFilter nationalId;
    private StringFilter agentNumber;
    private StringFilter vmtRef;
    private StringFilter linkStatus;
    private IntegerFilter status;
    private Boolean distinct;

    public LinkedWalletSubscriptionCriteria() {}

    public LinkedWalletSubscriptionCriteria(LinkedWalletSubscriptionCriteria other) {
        this.id = other.id;
        this.paymentType = other.paymentType;
        this.aliasName = other.aliasName;
        this.requestId = other.requestId;
        this.serviceType = other.serviceType;
        this.pspName = other.pspName;
        this.mobilePhoneNumber = other.mobilePhoneNumber;
        this.savingsAccountNumber = other.savingsAccountNumber;
        this.bankName = other.bankName;
        this.bankShortCode = other.bankShortCode;
        this.referenceNumber = other.referenceNumber;
        this.nationalId = other.nationalId;
        this.agentNumber = other.agentNumber;
        this.vmtRef = other.vmtRef;
        this.linkStatus = other.linkStatus;
        this.status = other.status;
        this.distinct = other.distinct;
    }

    public LongFilter getId() { return id; }
    public void setId(LongFilter id) { this.id = id; }

    public StringFilter getPaymentType() { return paymentType; }
    public void setPaymentType(StringFilter paymentType) { this.paymentType = paymentType; }

    public StringFilter getAliasName() { return aliasName; }
    public void setAliasName(StringFilter aliasName) { this.aliasName = aliasName; }

    public StringFilter getRequestId() { return requestId; }
    public void setRequestId(StringFilter requestId) { this.requestId = requestId; }

    public StringFilter getServiceType() { return serviceType; }
    public void setServiceType(StringFilter serviceType) { this.serviceType = serviceType; }

    public StringFilter getPspName() { return pspName; }
    public void setPspName(StringFilter pspName) { this.pspName = pspName; }

    public StringFilter getMobilePhoneNumber() { return mobilePhoneNumber; }
    public void setMobilePhoneNumber(StringFilter mobilePhoneNumber) { this.mobilePhoneNumber = mobilePhoneNumber; }

    public StringFilter getSavingsAccountNumber() { return savingsAccountNumber; }
    public void setSavingsAccountNumber(StringFilter savingsAccountNumber) { this.savingsAccountNumber = savingsAccountNumber; }

    public StringFilter getBankName() { return bankName; }
    public void setBankName(StringFilter bankName) { this.bankName = bankName; }

    public StringFilter getBankShortCode() { return bankShortCode; }
    public void setBankShortCode(StringFilter bankShortCode) { this.bankShortCode = bankShortCode; }

    public StringFilter getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(StringFilter referenceNumber) { this.referenceNumber = referenceNumber; }

    public StringFilter getNationalId() { return nationalId; }
    public void setNationalId(StringFilter nationalId) { this.nationalId = nationalId; }

    public StringFilter getAgentNumber() { return agentNumber; }
    public void setAgentNumber(StringFilter agentNumber) { this.agentNumber = agentNumber; }

    public StringFilter getVmtRef() { return vmtRef; }
    public void setVmtRef(StringFilter vmtRef) { this.vmtRef = vmtRef; }

    public StringFilter getLinkStatus() { return linkStatus; }
    public void setLinkStatus(StringFilter linkStatus) { this.linkStatus = linkStatus; }

    public IntegerFilter getStatus() { return status; }
    public void setStatus(IntegerFilter status) { this.status = status; }

    public Boolean getDistinct() { return distinct; }
    public void setDistinct(Boolean distinct) { this.distinct = distinct; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkedWalletSubscriptionCriteria that = (LinkedWalletSubscriptionCriteria) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(paymentType, that.paymentType) &&
            Objects.equals(aliasName, that.aliasName) &&
            Objects.equals(requestId, that.requestId) &&
            Objects.equals(serviceType, that.serviceType) &&
            Objects.equals(pspName, that.pspName) &&
            Objects.equals(mobilePhoneNumber, that.mobilePhoneNumber) &&
            Objects.equals(savingsAccountNumber, that.savingsAccountNumber) &&
            Objects.equals(bankName, that.bankName) &&
            Objects.equals(bankShortCode, that.bankShortCode) &&
            Objects.equals(referenceNumber, that.referenceNumber) &&
            Objects.equals(nationalId, that.nationalId) &&
            Objects.equals(agentNumber, that.agentNumber) &&
            Objects.equals(vmtRef, that.vmtRef) &&
            Objects.equals(linkStatus, that.linkStatus) &&
            Objects.equals(status, that.status) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, paymentType, aliasName, requestId, serviceType, pspName,
            mobilePhoneNumber, savingsAccountNumber, bankName, bankShortCode, referenceNumber,
            nationalId, agentNumber, vmtRef, linkStatus, status, distinct);
    }

    @Override
    public String toString() {
        return "LinkedWalletSubscriptionCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (paymentType != null ? "paymentType=" + paymentType + ", " : "") +
            (aliasName != null ? "aliasName=" + aliasName + ", " : "") +
            (requestId != null ? "requestId=" + requestId + ", " : "") +
            (serviceType != null ? "serviceType=" + serviceType + ", " : "") +
            (pspName != null ? "pspName=" + pspName + ", " : "") +
            (mobilePhoneNumber != null ? "mobilePhoneNumber=" + mobilePhoneNumber + ", " : "") +
            (savingsAccountNumber != null ? "savingsAccountNumber=" + savingsAccountNumber + ", " : "") +
            (bankName != null ? "bankName=" + bankName + ", " : "") +
            (bankShortCode != null ? "bankShortCode=" + bankShortCode + ", " : "") +
            (referenceNumber != null ? "referenceNumber=" + referenceNumber + ", " : "") +
            (nationalId != null ? "nationalId=" + nationalId + ", " : "") +
            (agentNumber != null ? "agentNumber=" + agentNumber + ", " : "") +
            (vmtRef != null ? "vmtRef=" + vmtRef + ", " : "") +
            (linkStatus != null ? "linkStatus=" + linkStatus + ", " : "") +
            (status != null ? "status=" + status + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            "}";
    }
}
