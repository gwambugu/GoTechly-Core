package com.dotechly.gotechly.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "PARTNER_ACCOUNTS")
public class PartnerAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "PARTNER_ID", nullable = false)
    private Long partnerId;

    @NotNull
    @Size(max = 50)
    @Column(name = "ACCOUNT_NUMBER", length = 50, nullable = false)
    private String accountNumber;

    @Size(max = 200)
    @Column(name = "ACCOUNT_NAME", length = 200)
    private String accountName;

    @NotNull
    @Size(max = 30)
    @Column(name = "ACCOUNT_TYPE", length = 30, nullable = false)
    private String accountType;

    @Size(max = 20)
    @Column(name = "BANK_CODE", length = 20)
    private String bankCode;

    @Size(max = 200)
    @Column(name = "BANK_NAME", length = 200)
    private String bankName;

    @Size(max = 20)
    @Column(name = "BRANCH_CODE", length = 20)
    private String branchCode;

    @Size(max = 11)
    @Column(name = "SWIFT_CODE", length = 11)
    private String swiftCode;

    @Size(max = 3)
    @Column(name = "COUNTRY_CODE", length = 3)
    private String countryCode;

    @NotNull
    @Size(max = 3)
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    @NotNull
    @Column(name = "IS_PRIMARY", nullable = false)
    private Boolean isPrimary = false;

    @NotNull
    @Size(max = 30)
    @Column(name = "STATUS", length = 30, nullable = false)
    private String status;

    @Size(max = 200)
    @Column(name = "MAKER_ID", length = 200)
    private String makerId;

    @Column(name = "MAKER_DATE")
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

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
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

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartnerAccount that = (PartnerAccount) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PartnerAccount{" +
            "id=" + getId() +
            ", partnerId=" + getPartnerId() +
            ", accountNumber='" + getAccountNumber() + "'" +
            ", accountType='" + getAccountType() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }
}
