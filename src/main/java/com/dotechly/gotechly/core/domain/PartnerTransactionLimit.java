package com.dotechly.gotechly.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "PARTNER_TRANSACTION_LIMITS")
public class PartnerTransactionLimit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "PARTNER_ID", nullable = false)
    private Long partnerId;

    @NotNull
    @Size(max = 100)
    @Column(name = "PAYMENT_TYPE", length = 100, nullable = false)
    private String paymentType;

    @Size(max = 20)
    @Column(name = "DIRECTION", length = 20)
    private String direction;

    @NotNull
    @Size(max = 3)
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "MIN_AMOUNT", precision = 21, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "MAX_AMOUNT", precision = 21, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "DAILY_AMOUNT_LIMIT", precision = 21, scale = 2)
    private BigDecimal dailyAmountLimit;

    @Column(name = "DAILY_COUNT_LIMIT")
    private Integer dailyCountLimit;

    @Column(name = "MONTHLY_AMOUNT_LIMIT", precision = 21, scale = 2)
    private BigDecimal monthlyAmountLimit;

    @Column(name = "MONTHLY_COUNT_LIMIT")
    private Integer monthlyCountLimit;

    @Column(name = "FEE_AMOUNT", precision = 21, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "FEE_PERCENTAGE", precision = 8, scale = 4)
    private BigDecimal feePercentage;

    @NotNull
    @Size(max = 30)
    @Column(name = "STATUS", length = 30, nullable = false)
    private String status;

    @Column(name = "EFFECTIVE_DATE")
    private LocalDate effectiveDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDate expiryDate;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public BigDecimal getDailyAmountLimit() { return dailyAmountLimit; }
    public void setDailyAmountLimit(BigDecimal dailyAmountLimit) { this.dailyAmountLimit = dailyAmountLimit; }

    public Integer getDailyCountLimit() { return dailyCountLimit; }
    public void setDailyCountLimit(Integer dailyCountLimit) { this.dailyCountLimit = dailyCountLimit; }

    public BigDecimal getMonthlyAmountLimit() { return monthlyAmountLimit; }
    public void setMonthlyAmountLimit(BigDecimal monthlyAmountLimit) { this.monthlyAmountLimit = monthlyAmountLimit; }

    public Integer getMonthlyCountLimit() { return monthlyCountLimit; }
    public void setMonthlyCountLimit(Integer monthlyCountLimit) { this.monthlyCountLimit = monthlyCountLimit; }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }

    public BigDecimal getFeePercentage() { return feePercentage; }
    public void setFeePercentage(BigDecimal feePercentage) { this.feePercentage = feePercentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartnerTransactionLimit that = (PartnerTransactionLimit) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "PartnerTransactionLimit{" +
            "id=" + getId() +
            ", partnerId=" + getPartnerId() +
            ", paymentType='" + getPaymentType() + "'" +
            ", currencyCode='" + getCurrencyCode() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }
}
