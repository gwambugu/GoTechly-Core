package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.service.filter.LongFilter;
import com.dotechly.gotechly.core.service.filter.StringFilter;

import java.io.Serializable;
import java.util.Objects;

public class PartnerCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter partnerCode;

    private StringFilter clientShortName;

    private StringFilter legalName;

    private StringFilter agentId;

    private StringFilter partnerType;

    private StringFilter countryCode;

    private StringFilter contactEmail;

    private StringFilter contactPhone;

    private StringFilter status;

    private StringFilter kycStatus;

    private Boolean distinct;

    public PartnerCriteria() {}

    public PartnerCriteria(PartnerCriteria other) {
        this.id = other.id;
        this.partnerCode = other.partnerCode;
        this.clientShortName = other.clientShortName;
        this.legalName = other.legalName;
        this.agentId = other.agentId;
        this.partnerType = other.partnerType;
        this.countryCode = other.countryCode;
        this.contactEmail = other.contactEmail;
        this.contactPhone = other.contactPhone;
        this.status = other.status;
        this.kycStatus = other.kycStatus;
        this.distinct = other.distinct;
    }

    public LongFilter getId() { return id; }
    public void setId(LongFilter id) { this.id = id; }

    public StringFilter getPartnerCode() { return partnerCode; }
    public void setPartnerCode(StringFilter partnerCode) { this.partnerCode = partnerCode; }

    public StringFilter getClientShortName() { return clientShortName; }
    public void setClientShortName(StringFilter clientShortName) { this.clientShortName = clientShortName; }

    public StringFilter getLegalName() { return legalName; }
    public void setLegalName(StringFilter legalName) { this.legalName = legalName; }

    public StringFilter getAgentId() { return agentId; }
    public void setAgentId(StringFilter agentId) { this.agentId = agentId; }

    public StringFilter getPartnerType() { return partnerType; }
    public void setPartnerType(StringFilter partnerType) { this.partnerType = partnerType; }

    public StringFilter getCountryCode() { return countryCode; }
    public void setCountryCode(StringFilter countryCode) { this.countryCode = countryCode; }

    public StringFilter getContactEmail() { return contactEmail; }
    public void setContactEmail(StringFilter contactEmail) { this.contactEmail = contactEmail; }

    public StringFilter getContactPhone() { return contactPhone; }
    public void setContactPhone(StringFilter contactPhone) { this.contactPhone = contactPhone; }

    public StringFilter getStatus() { return status; }
    public void setStatus(StringFilter status) { this.status = status; }

    public StringFilter getKycStatus() { return kycStatus; }
    public void setKycStatus(StringFilter kycStatus) { this.kycStatus = kycStatus; }

    public Boolean getDistinct() { return distinct; }
    public void setDistinct(Boolean distinct) { this.distinct = distinct; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartnerCriteria that = (PartnerCriteria) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(partnerCode, that.partnerCode) &&
            Objects.equals(clientShortName, that.clientShortName) &&
            Objects.equals(legalName, that.legalName) &&
            Objects.equals(agentId, that.agentId) &&
            Objects.equals(partnerType, that.partnerType) &&
            Objects.equals(countryCode, that.countryCode) &&
            Objects.equals(contactEmail, that.contactEmail) &&
            Objects.equals(contactPhone, that.contactPhone) &&
            Objects.equals(status, that.status) &&
            Objects.equals(kycStatus, that.kycStatus) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, partnerCode, clientShortName, legalName, agentId,
            partnerType, countryCode, contactEmail, contactPhone, status, kycStatus, distinct);
    }

    @Override
    public String toString() {
        return "PartnerCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (partnerCode != null ? "partnerCode=" + partnerCode + ", " : "") +
            (clientShortName != null ? "clientShortName=" + clientShortName + ", " : "") +
            (legalName != null ? "legalName=" + legalName + ", " : "") +
            (agentId != null ? "agentId=" + agentId + ", " : "") +
            (partnerType != null ? "partnerType=" + partnerType + ", " : "") +
            (countryCode != null ? "countryCode=" + countryCode + ", " : "") +
            (contactEmail != null ? "contactEmail=" + contactEmail + ", " : "") +
            (contactPhone != null ? "contactPhone=" + contactPhone + ", " : "") +
            (status != null ? "status=" + status + ", " : "") +
            (kycStatus != null ? "kycStatus=" + kycStatus + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            "}";
    }
}
