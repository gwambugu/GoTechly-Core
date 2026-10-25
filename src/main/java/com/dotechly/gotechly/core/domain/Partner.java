package com.dotechly.gotechly.core.domain;

import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "PARTNERS")
public class Partner implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "PARTNER_CODE", length = 50, nullable = false, unique = true)
    private String partnerCode;

    @NotNull
    @Size(max = 100)
    @Column(name = "CLIENT_SHORT_NAME", length = 100, nullable = false, unique = true)
    private String clientShortName;

    @NotNull
    @Size(max = 300)
    @Column(name = "LEGAL_NAME", length = 300, nullable = false)
    private String legalName;

    @NotNull
    @Size(max = 100)
    @Column(name = "AGENT_ID", length = 100, nullable = false, unique = true)
    private String agentId;

    @NotNull
    @Size(max = 50)
    @Column(name = "PARTNER_TYPE", length = 50, nullable = false)
    private String partnerType;

    @Size(max = 3)
    @Column(name = "COUNTRY_CODE", length = 3)
    private String countryCode;

    @NotNull
    @Size(max = 200)
    @Column(name = "CONTACT_EMAIL", length = 200, nullable = false, unique = true)
    private String contactEmail;

    @Size(max = 20)
    @Column(name = "CONTACT_PHONE", length = 20, unique = true)
    private String contactPhone;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "CLIENT_CERTIFICATE")
    private String clientCertificate;

    @Size(max = 500)
    @Column(name = "SALT", length = 500)
    private String salt;

    @Size(max = 500)
    @Column(name = "CALLBACK_URL", length = 500)
    private String callbackUrl;

    @Size(max = 1000)
    @Column(name = "IP_WHITELIST", length = 1000)
    private String ipWhitelist;

    @Size(max = 20)
    @Column(name = "SETTLEMENT_CYCLE", length = 20)
    private String settlementCycle;

    @Column(name = "CONTRACT_START_DATE")
    private LocalDate contractStartDate;

    @Column(name = "CONTRACT_END_DATE")
    private LocalDate contractEndDate;

    @Size(max = 20)
    @Column(name = "KYC_STATUS", length = 20)
    private String kycStatus;

    @Column(name = "KYC_EXPIRY_DATE")
    private LocalDate kycExpiryDate;

    @NotNull
    @Size(max = 30)
    @Column(name = "STATUS", length = 30, nullable = false)
    private String status;

    @NotNull
    @Size(max = 200)
    @Column(name = "MAKER_ID", length = 200, nullable = false)
    private String makerId;

    @NotNull
    @Column(name = "MAKER_DATE", nullable = false)
    private Instant makerDate;

    @Size(max = 200)
    @Column(name = "CHECKER_ID", length = 200)
    private String checkerId;

    @Column(name = "CHECKER_DATE")
    private Instant checkerDate;

    @Size(max = 500)
    @Column(name = "CHECKER_REMARKS", length = 500)
    private String checkerRemarks;

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

    @Size(max = 64)
    @Column(name = "HASH1", length = 64)
    private String hash1;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public String getClientShortName() {
        return clientShortName;
    }

    public void setClientShortName(String clientShortName) {
        this.clientShortName = clientShortName;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getPartnerType() {
        return partnerType;
    }

    public void setPartnerType(String partnerType) {
        this.partnerType = partnerType;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getClientCertificate() {
        return clientCertificate;
    }

    public void setClientCertificate(String clientCertificate) {
        this.clientCertificate = clientCertificate;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(String ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }

    public String getSettlementCycle() {
        return settlementCycle;
    }

    public void setSettlementCycle(String settlementCycle) {
        this.settlementCycle = settlementCycle;
    }

    public LocalDate getContractStartDate() {
        return contractStartDate;
    }

    public void setContractStartDate(LocalDate contractStartDate) {
        this.contractStartDate = contractStartDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
    }

    public void setContractEndDate(LocalDate contractEndDate) {
        this.contractEndDate = contractEndDate;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public LocalDate getKycExpiryDate() {
        return kycExpiryDate;
    }

    public void setKycExpiryDate(LocalDate kycExpiryDate) {
        this.kycExpiryDate = kycExpiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMakerId() {
        return makerId;
    }

    public void setMakerId(String makerId) {
        this.makerId = makerId;
    }

    public Instant getMakerDate() {
        return makerDate;
    }

    public void setMakerDate(Instant makerDate) {
        this.makerDate = makerDate;
    }

    public String getCheckerId() {
        return checkerId;
    }

    public void setCheckerId(String checkerId) {
        this.checkerId = checkerId;
    }

    public Instant getCheckerDate() {
        return checkerDate;
    }

    public void setCheckerDate(Instant checkerDate) {
        this.checkerDate = checkerDate;
    }

    public String getCheckerRemarks() {
        return checkerRemarks;
    }

    public void setCheckerRemarks(String checkerRemarks) {
        this.checkerRemarks = checkerRemarks;
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

    public String getHash1() {
        return hash1;
    }

    public void setHash1(String hash1) {
        this.hash1 = hash1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Partner that = (Partner) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Partner{" +
            "id=" + getId() +
            ", partnerCode='" + getPartnerCode() + "'" +
            ", clientShortName='" + getClientShortName() + "'" +
            ", agentId='" + getAgentId() + "'" +
            ", partnerType='" + getPartnerType() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }
}
