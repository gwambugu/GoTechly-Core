package com.dotechly.gotechly.core.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class PartnerAccountDTO implements Serializable {

    private Long id;

    @NotNull
    private Long partnerId;

    @NotBlank
    @Size(max = 50)
    private String accountNumber;

    @Size(max = 200)
    private String accountName;

    @NotBlank
    @Size(max = 30)
    private String accountType;

    @Size(max = 20)
    private String bankCode;

    @Size(max = 200)
    private String bankName;

    @Size(max = 20)
    private String branchCode;

    @Size(max = 11)
    private String swiftCode;

    @Size(max = 3)
    private String countryCode;

    @NotBlank
    @Size(max = 3)
    private String currencyCode;

    private Boolean isPrimary;

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

    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getSwiftCode() { return swiftCode; }
    public void setSwiftCode(String swiftCode) { this.swiftCode = swiftCode; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }

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
        PartnerAccountDTO that = (PartnerAccountDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PartnerAccountDTO{" +
            "id=" + id +
            ", partnerId=" + partnerId +
            ", accountNumber='" + accountNumber + '\'' +
            ", accountType='" + accountType + '\'' +
            ", currencyCode='" + currencyCode + '\'' +
            ", status='" + status + '\'' +
            '}';
    }
}
