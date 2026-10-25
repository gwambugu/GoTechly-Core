package com.dotechly.gotechly.core.service.dto;

import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class BankCodesLookupDTO implements Serializable {

    private Long id;

    @Size(max = 5)
    private String countryCode;

    @Size(max = 5)
    private String bankCode;

    @Size(max = 100)
    private String bankName;

    @Size(max = 30)
    private String bankShortName;

    @Size(max = 5)
    private String bankBranchCode;

    @Size(max = 50)
    private String bic;

    @Size(max = 20)
    private String pic;

    @Size(max = 20)
    private String bin;

    private Map<String, String> additionalFields;

    @Size(max = 20)
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankShortName() { return bankShortName; }
    public void setBankShortName(String bankShortName) { this.bankShortName = bankShortName; }

    public String getBankBranchCode() { return bankBranchCode; }
    public void setBankBranchCode(String bankBranchCode) { this.bankBranchCode = bankBranchCode; }

    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic; }

    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }

    public String getBin() { return bin; }
    public void setBin(String bin) { this.bin = bin; }

    public Map<String, String> getAdditionalFields() { return additionalFields; }
    public void setAdditionalFields(Map<String, String> additionalFields) { this.additionalFields = additionalFields; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankCodesLookupDTO that = (BankCodesLookupDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BankCodesLookupDTO{" +
                "id=" + id +
                ", countryCode='" + countryCode + '\'' +
                ", bankCode='" + bankCode + '\'' +
                ", bankName='" + bankName + '\'' +
                ", bankShortName='" + bankShortName + '\'' +
                ", bankBranchCode='" + bankBranchCode + '\'' +
                ", bic='" + bic + '\'' +
                '}';
    }
}