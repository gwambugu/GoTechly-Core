package com.dotechly.gotechly.core.service.dto;

import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/**
 * DTO for {@link com.dotechly.gotechly.core.domain.LinkedWalletSubscription}. Writes flow
 * through the maker-checker approval path (entityType {@code LINKED_WALLET_SUBSCRIPTION}).
 */
public class LinkedWalletSubscriptionDTO implements Serializable {

    private Long id;

    @Size(max = 50)
    private String paymentType;

    @Size(max = 35)
    private String aliasName;

    @Size(max = 70)
    private String requestId;

    private Instant dateLinked;

    @Size(max = 100)
    private String serviceType;

    @Size(max = 100)
    private String pspName;

    @Size(max = 15)
    private String mobilePhoneNumber;

    @Size(max = 16)
    private String savingsAccountNumber;

    @Size(max = 16)
    private String operatingCurrencyCode;

    @Size(max = 200)
    private String firstName;

    @Size(max = 200)
    private String middleName;

    @Size(max = 200)
    private String lastName;

    private LocalDate dateOfBirth;

    @Size(max = 20)
    private String nationalId;

    @Size(max = 50)
    private String passportNumber;

    @Size(max = 3)
    private String countryCode;

    @Size(max = 200)
    private String cityTown;

    @Size(max = 200)
    private String address1;

    @Size(max = 200)
    private String address2;

    @Size(max = 200)
    private String postalCode;

    @Size(max = 200)
    private String postalAddress;

    private LocalDate openingDate;

    @Size(max = 50)
    private String agentNumber;

    @Size(max = 80)
    private String agentName;

    @Size(max = 100)
    private String bankName;

    @Size(max = 50)
    private String bankShortCode;

    @Size(max = 100)
    private String referenceNumber;

    @Size(max = 6)
    private String actionCode;

    private Integer status;

    @Size(max = 200)
    private String remarks;

    @Size(max = 10)
    private String pspDob;

    @Size(max = 15)
    private String pspNationalId;

    @Size(max = 15)
    private String pspPhone;

    private LocalDate createDate;

    @Size(max = 20)
    private String linkStatus;

    @Size(max = 10)
    private String linkTranId;

    private Long vmtRefId;

    @Size(max = 50)
    private String vmtRef;

    @Size(max = 10)
    private String creditQualified;

    @Size(max = 50)
    private String loanAccount;

    private BigDecimal loanBalance;

    @Size(max = 200)
    private String hash1;

    @Size(max = 200)
    private String hash2;

    private Map<String, String> additionalFields;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Instant getDateLinked() { return dateLinked; }
    public void setDateLinked(Instant dateLinked) { this.dateLinked = dateLinked; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getPspName() { return pspName; }
    public void setPspName(String pspName) { this.pspName = pspName; }

    public String getMobilePhoneNumber() { return mobilePhoneNumber; }
    public void setMobilePhoneNumber(String mobilePhoneNumber) { this.mobilePhoneNumber = mobilePhoneNumber; }

    public String getSavingsAccountNumber() { return savingsAccountNumber; }
    public void setSavingsAccountNumber(String savingsAccountNumber) { this.savingsAccountNumber = savingsAccountNumber; }

    public String getOperatingCurrencyCode() { return operatingCurrencyCode; }
    public void setOperatingCurrencyCode(String operatingCurrencyCode) { this.operatingCurrencyCode = operatingCurrencyCode; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCityTown() { return cityTown; }
    public void setCityTown(String cityTown) { this.cityTown = cityTown; }

    public String getAddress1() { return address1; }
    public void setAddress1(String address1) { this.address1 = address1; }

    public String getAddress2() { return address2; }
    public void setAddress2(String address2) { this.address2 = address2; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getPostalAddress() { return postalAddress; }
    public void setPostalAddress(String postalAddress) { this.postalAddress = postalAddress; }

    public LocalDate getOpeningDate() { return openingDate; }
    public void setOpeningDate(LocalDate openingDate) { this.openingDate = openingDate; }

    public String getAgentNumber() { return agentNumber; }
    public void setAgentNumber(String agentNumber) { this.agentNumber = agentNumber; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankShortCode() { return bankShortCode; }
    public void setBankShortCode(String bankShortCode) { this.bankShortCode = bankShortCode; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getActionCode() { return actionCode; }
    public void setActionCode(String actionCode) { this.actionCode = actionCode; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getPspDob() { return pspDob; }
    public void setPspDob(String pspDob) { this.pspDob = pspDob; }

    public String getPspNationalId() { return pspNationalId; }
    public void setPspNationalId(String pspNationalId) { this.pspNationalId = pspNationalId; }

    public String getPspPhone() { return pspPhone; }
    public void setPspPhone(String pspPhone) { this.pspPhone = pspPhone; }

    public LocalDate getCreateDate() { return createDate; }
    public void setCreateDate(LocalDate createDate) { this.createDate = createDate; }

    public String getLinkStatus() { return linkStatus; }
    public void setLinkStatus(String linkStatus) { this.linkStatus = linkStatus; }

    public String getLinkTranId() { return linkTranId; }
    public void setLinkTranId(String linkTranId) { this.linkTranId = linkTranId; }

    public Long getVmtRefId() { return vmtRefId; }
    public void setVmtRefId(Long vmtRefId) { this.vmtRefId = vmtRefId; }

    public String getVmtRef() { return vmtRef; }
    public void setVmtRef(String vmtRef) { this.vmtRef = vmtRef; }

    public String getCreditQualified() { return creditQualified; }
    public void setCreditQualified(String creditQualified) { this.creditQualified = creditQualified; }

    public String getLoanAccount() { return loanAccount; }
    public void setLoanAccount(String loanAccount) { this.loanAccount = loanAccount; }

    public BigDecimal getLoanBalance() { return loanBalance; }
    public void setLoanBalance(BigDecimal loanBalance) { this.loanBalance = loanBalance; }

    public String getHash1() { return hash1; }
    public void setHash1(String hash1) { this.hash1 = hash1; }

    public String getHash2() { return hash2; }
    public void setHash2(String hash2) { this.hash2 = hash2; }

    public Map<String, String> getAdditionalFields() { return additionalFields; }
    public void setAdditionalFields(Map<String, String> additionalFields) { this.additionalFields = additionalFields; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkedWalletSubscriptionDTO that = (LinkedWalletSubscriptionDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LinkedWalletSubscriptionDTO{" +
            "id=" + id +
            ", paymentType='" + paymentType + '\'' +
            ", requestId='" + requestId + '\'' +
            ", aliasName='" + aliasName + '\'' +
            ", linkStatus='" + linkStatus + '\'' +
            ", serviceType='" + serviceType + '\'' +
            ", pspName='" + pspName + '\'' +
            ", mobilePhoneNumber='" + mobilePhoneNumber + '\'' +
            ", savingsAccountNumber='" + savingsAccountNumber + '\'' +
            ", bankShortCode='" + bankShortCode + '\'' +
            ", vmtRef='" + vmtRef + '\'' +
            '}';
    }
}
