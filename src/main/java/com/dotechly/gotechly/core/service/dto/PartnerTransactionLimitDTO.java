package com.dotechly.gotechly.core.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class PartnerTransactionLimitDTO implements Serializable {

    private Long id;

    @NotNull
    private Long partnerId;

    @NotBlank
    @Size(max = 100)
    private String paymentType;

    @Size(max = 20)
    private String direction;

    @NotBlank
    @Size(max = 3)
    private String currencyCode;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private BigDecimal dailyAmountLimit;

    private Integer dailyCountLimit;

    private BigDecimal monthlyAmountLimit;

    private Integer monthlyCountLimit;

    private BigDecimal feeAmount;

    private BigDecimal feePercentage;

    @Size(max = 30)
    private String status;

    private LocalDate effectiveDate;

    private LocalDate expiryDate;

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
        PartnerTransactionLimitDTO that = (PartnerTransactionLimitDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PartnerTransactionLimitDTO{" +
            "id=" + id +
            ", partnerId=" + partnerId +
            ", paymentType='" + paymentType + '\'' +
            ", direction='" + direction + '\'' +
            ", currencyCode='" + currencyCode + '\'' +
            ", status='" + status + '\'' +
            '}';
    }
}
