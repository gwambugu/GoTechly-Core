package com.dotechly.gotechly.core.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class PartnerDTO implements Serializable {

    private Long id;

    @Size(max = 50)
    private String partnerCode;

    @NotBlank
    @Size(max = 100)
    private String clientShortName;

    @NotBlank
    @Size(max = 300)
    private String legalName;

    @NotBlank
    @Size(max = 100)
    private String agentId;

    @NotBlank
    @Size(max = 50)
    private String partnerType;

    @Size(max = 3)
    private String countryCode;

    @NotBlank
    @Email
    @Size(max = 200)
    private String contactEmail;

    @Size(max = 20)
    private String contactPhone;

    private String clientCertificate;

    private String salt;

    @Size(max = 500)
    private String callbackUrl;

    @Size(max = 1000)
    private String ipWhitelist;

    @Size(max = 20)
    private String settlementCycle;

    private LocalDate contractStartDate;

    private LocalDate contractEndDate;

    @Size(max = 20)
    private String kycStatus;

    private LocalDate kycExpiryDate;

    @Size(max = 30)
    private String status;

    @Size(max = 200)
    private String makerId;

    private Instant makerDate;

    @Size(max = 200)
    private String checkerId;

    private Instant checkerDate;

    @Size(max = 500)
    private String checkerRemarks;

    private Instant dateCreated;

    @Size(max = 200)
    private String createdBy;

    private Instant dateModified;

    @Size(max = 200)
    private String modifiedBy;

    @Size(max = 64)
    private String hash1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public String getClientShortName() { return clientShortName; }
    public void setClientShortName(String clientShortName) { this.clientShortName = clientShortName; }

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getPartnerType() { return partnerType; }
    public void setPartnerType(String partnerType) { this.partnerType = partnerType; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getClientCertificate() { return clientCertificate; }
    public void setClientCertificate(String clientCertificate) { this.clientCertificate = clientCertificate; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    public String getIpWhitelist() { return ipWhitelist; }
    public void setIpWhitelist(String ipWhitelist) { this.ipWhitelist = ipWhitelist; }

    public String getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(String settlementCycle) { this.settlementCycle = settlementCycle; }

    public LocalDate getContractStartDate() { return contractStartDate; }
    public void setContractStartDate(LocalDate contractStartDate) { this.contractStartDate = contractStartDate; }

    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public LocalDate getKycExpiryDate() { return kycExpiryDate; }
    public void setKycExpiryDate(LocalDate kycExpiryDate) { this.kycExpiryDate = kycExpiryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMakerId() { return makerId; }
    public void setMakerId(String makerId) { this.makerId = makerId; }

    public Instant getMakerDate() { return makerDate; }
    public void setMakerDate(Instant makerDate) { this.makerDate = makerDate; }

    public String getCheckerId() { return checkerId; }
    public void setCheckerId(String checkerId) { this.checkerId = checkerId; }

    public Instant getCheckerDate() { return checkerDate; }
    public void setCheckerDate(Instant checkerDate) { this.checkerDate = checkerDate; }

    public String getCheckerRemarks() { return checkerRemarks; }
    public void setCheckerRemarks(String checkerRemarks) { this.checkerRemarks = checkerRemarks; }

    public Instant getDateCreated() { return dateCreated; }
    public void setDateCreated(Instant dateCreated) { this.dateCreated = dateCreated; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getDateModified() { return dateModified; }
    public void setDateModified(Instant dateModified) { this.dateModified = dateModified; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    public String getHash1() { return hash1; }
    public void setHash1(String hash1) { this.hash1 = hash1; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartnerDTO that = (PartnerDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PartnerDTO{" +
            "id=" + id +
            ", partnerCode='" + partnerCode + '\'' +
            ", clientShortName='" + clientShortName + '\'' +
            ", agentId='" + agentId + '\'' +
            ", partnerType='" + partnerType + '\'' +
            ", status='" + status + '\'' +
            '}';
    }
}
