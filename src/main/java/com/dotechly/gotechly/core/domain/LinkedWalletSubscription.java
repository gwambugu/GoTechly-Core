package com.dotechly.gotechly.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/**
 * A customer subscribed to the linked-wallet &harr; bank-account service (e.g. M-KESHO): the
 * savings account a PSP wallet is linked to, plus KYC / linkage / credit metadata.
 *
 * <p>Edited only through the maker-checker flow ({@code PendingChangeService}, entityType
 * {@code LINKED_WALLET_SUBSCRIPTION}); approvals are audited. {@code additionalFields} is JSON in a
 * CLOB column, surfaced as {@code Map<String,String>} via {@link JsonMapConverter} — same wiring as
 * {@code BankCodesLookup.additionalFields} / {@code Channels.additionalFields}.
 */
@Entity
@Table(name = "LINKED_WALLET_SUBSCRIPTION")
public class LinkedWalletSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    /**
     * The scheme/corridor that owns this linkage (e.g. {@code RNDPS}) — the key
     * {@code LinkedAccountService} resolves the plugin and the status API from. Distinct from
     * {@code serviceType}/{@code pspName}, which carry the M-KESHO (VMT) vocabulary.
     */
    @Size(max = 50)
    @Column(name = "PAYMENT_TYPE", length = 50)
    private String paymentType;

    /** The alias / proxy the customer chose ("eKash-Id" in the R-NDPS spec). */
    @Size(max = 35)
    @Column(name = "ALIAS_NAME", length = 35)
    private String aliasName;

    /** The channel's correlation id for the linkage request. Unique — a replay must collide. */
    @Size(max = 70)
    @Column(name = "REQUEST_ID", length = 70)
    private String requestId;

    /**
     * When the linkage came into force. Null while the scheme has only acknowledged the request:
     * nothing is linked yet, and {@code createDate} answers a different question (row created).
     *
     * <p><strong>{@code @JdbcTypeCode(Types.TIMESTAMP)} is load-bearing on Oracle.</strong> Hibernate 6+
     * maps {@link Instant} to {@code TIMESTAMP_UTC} by default, which the Oracle dialect reads with
     * {@code getObject(col, OffsetDateTime.class)}. Against a column declared as Oracle {@code DATE} —
     * which carries no zone — the driver answers <em>ORA-18716: not in any time zone</em>, and because
     * it fires during result-set extraction the whole listing 500s rather than one row degrading. The
     * explicit code makes Hibernate read and write a plain {@code TIMESTAMP} instead, which works
     * against both {@code DATE} and {@code TIMESTAMP} columns, so it is correct whichever way a given
     * environment's schema was created — the Liquibase changeset asks for {@code timestamp}, but the
     * GoTechly Oracle instance reported {@code DATE}, so do not assume the declared type is what is there.
     * Scoped to this one field on purpose: every other {@code Instant} column in this schema reads
     * fine today, and widening the change would risk shifting values that are currently correct.
     */
    @Column(name = "DATE_LINKED")
    @JdbcTypeCode(Types.TIMESTAMP)
    private Instant dateLinked;

    @Size(max = 100)
    @Column(name = "SERVICE_TYPE", length = 100)
    private String serviceType;

    @Size(max = 100)
    @Column(name = "PSP_NAME", length = 100)
    private String pspName;

    @Size(max = 15)
    @Column(name = "MOBILE_PHONENUMBER", length = 15)
    private String mobilePhoneNumber;

    @Size(max = 16)
    @Column(name = "SAVINGS_ACCOUNTNUMBER", length = 16)
    private String savingsAccountNumber;

    @Size(max = 16)
    @Column(name = "OPERATING_CURRENCY_CODE", length = 16)
    private String operatingCurrencyCode;

    @Size(max = 200)
    @Column(name = "FIRSTNAME", length = 200)
    private String firstName;

    @Size(max = 200)
    @Column(name = "MIDDLENAME", length = 200)
    private String middleName;

    @Size(max = 200)
    @Column(name = "LASTNAME", length = 200)
    private String lastName;

    @Column(name = "DATEOFBIRTH")
    private LocalDate dateOfBirth;

    @Size(max = 20)
    @Column(name = "NATIONALID", length = 20)
    private String nationalId;

    @Size(max = 50)
    @Column(name = "PASSPORT_NUMBER", length = 50)
    private String passportNumber;

    @Size(max = 3)
    @Column(name = "COUNTRY_CODE", length = 3)
    private String countryCode;

    @Size(max = 200)
    @Column(name = "CITY_TOWN", length = 200)
    private String cityTown;

    @Size(max = 200)
    @Column(name = "ADDRESS1", length = 200)
    private String address1;

    @Size(max = 200)
    @Column(name = "ADDRESS2", length = 200)
    private String address2;

    @Size(max = 200)
    @Column(name = "POSTALCODE", length = 200)
    private String postalCode;

    @Size(max = 200)
    @Column(name = "POSTAL_ADDRESS", length = 200)
    private String postalAddress;

    @Column(name = "OPENING_DATE")
    private LocalDate openingDate;

    @Size(max = 50)
    @Column(name = "AGENT_NUMBER", length = 50)
    private String agentNumber;

    @Size(max = 80)
    @Column(name = "AGENT_NAME", length = 80)
    private String agentName;

    @Size(max = 100)
    @Column(name = "BANK_NAME", length = 100)
    private String bankName;

    @Size(max = 50)
    @Column(name = "BANK_SHORTCODE", length = 50)
    private String bankShortCode;

    @Size(max = 100)
    @Column(name = "REFERENCE_NUMBER", length = 100)
    private String referenceNumber;

    @Size(max = 6)
    @Column(name = "ACTION_CODE", length = 6)
    private String actionCode;

    @Column(name = "STATUS")
    private Integer status;

    @Size(max = 200)
    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @Size(max = 10)
    @Column(name = "PSP_DOB", length = 10)
    private String pspDob;

    @Size(max = 15)
    @Column(name = "PSP_NATIONAL_ID", length = 15)
    private String pspNationalId;

    @Size(max = 15)
    @Column(name = "PSP_PHONE", length = 15)
    private String pspPhone;

    @Column(name = "CREATE_DATE")
    private LocalDate createDate;

    /** Canonical linkage state — see {@code LinkedAccountStatus}. */
    @Size(max = 20)
    @Column(name = "LINK_STATUS", length = 20)
    private String linkStatus;

    @Size(max = 10)
    @Column(name = "LINK_TRAN_ID", length = 10)
    private String linkTranId;

    @Column(name = "VMTREF_ID")
    private Long vmtRefId;

    @Size(max = 50)
    @Column(name = "VMTREF", length = 50)
    private String vmtRef;

    @Size(max = 10)
    @Column(name = "CREDIT_QUALIFIED", length = 10)
    private String creditQualified;

    @Size(max = 50)
    @Column(name = "LOAN_ACCOUNT", length = 50)
    private String loanAccount;

    @Column(name = "LOAN_BALANCE")
    private BigDecimal loanBalance;

    @Size(max = 200)
    @Column(name = "HASH1", length = 200)
    private String hash1;

    @Size(max = 200)
    @Column(name = "HASH2", length = 200)
    private String hash2;

    @Convert(converter = JsonMapConverter.class)
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "ADDITIONAL_FIELDS")
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
        LinkedWalletSubscription that = (LinkedWalletSubscription) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "LinkedWalletSubscription{" +
            "id=" + id +
            ", paymentType='" + paymentType + '\'' +
            ", requestId='" + requestId + '\'' +
            ", aliasName='" + aliasName + '\'' +
            ", serviceType='" + serviceType + '\'' +
            ", pspName='" + pspName + '\'' +
            ", mobilePhoneNumber='" + mobilePhoneNumber + '\'' +
            ", savingsAccountNumber='" + savingsAccountNumber + '\'' +
            ", bankName='" + bankName + '\'' +
            ", bankShortCode='" + bankShortCode + '\'' +
            ", vmtRef='" + vmtRef + '\'' +
            ", linkStatus='" + linkStatus + '\'' +
            ", status=" + status +
            '}';
    }
}
