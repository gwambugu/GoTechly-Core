package com.dotechly.gotechly.core.service.criteria;



import com.dotechly.gotechly.core.service.filter.LongFilter;
import com.dotechly.gotechly.core.service.filter.StringFilter;

import java.io.Serializable;
import java.util.Objects;

public class BankCodesLookupCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;
    private StringFilter countryCode;
    private StringFilter bankCode;
    private StringFilter bankName;
    private StringFilter bankShortName;
    private StringFilter bankBranchCode;
    private StringFilter bic;
    private StringFilter pic;
    private StringFilter bin;
    private StringFilter status;
    private Boolean distinct;

    public BankCodesLookupCriteria() {}

    public BankCodesLookupCriteria(BankCodesLookupCriteria other) {
        this.id = other.id;
        this.countryCode = other.countryCode;
        this.bankCode = other.bankCode;
        this.bankName = other.bankName;
        this.bankShortName = other.bankShortName;
        this.bankBranchCode = other.bankBranchCode;
        this.bic = other.bic;
        this.pic = other.pic;
        this.bin = other.bin;
        this.status = other.status;
        this.distinct = other.distinct;
    }

    public LongFilter getId() { return id; }
    public void setId(LongFilter id) { this.id = id; }

    public StringFilter getCountryCode() { return countryCode; }
    public void setCountryCode(StringFilter countryCode) { this.countryCode = countryCode; }

    public StringFilter getBankCode() { return bankCode; }
    public void setBankCode(StringFilter bankCode) { this.bankCode = bankCode; }

    public StringFilter getBankName() { return bankName; }
    public void setBankName(StringFilter bankName) { this.bankName = bankName; }

    public StringFilter getBankShortName() { return bankShortName; }
    public void setBankShortName(StringFilter bankShortName) { this.bankShortName = bankShortName; }

    public StringFilter getBankBranchCode() { return bankBranchCode; }
    public void setBankBranchCode(StringFilter bankBranchCode) { this.bankBranchCode = bankBranchCode; }

    public StringFilter getBic() { return bic; }
    public void setBic(StringFilter bic) { this.bic = bic; }

    public StringFilter getPic() { return pic; }
    public void setPic(StringFilter pic) { this.pic = pic; }

    public StringFilter getBin() { return bin; }
    public void setBin(StringFilter bin) { this.bin = bin; }

    public StringFilter getStatus() { return status; }
    public void setStatus(StringFilter status) { this.status = status; }

    public Boolean getDistinct() { return distinct; }
    public void setDistinct(Boolean distinct) { this.distinct = distinct; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankCodesLookupCriteria that = (BankCodesLookupCriteria) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(countryCode, that.countryCode) &&
                Objects.equals(bankCode, that.bankCode) &&
                Objects.equals(bankName, that.bankName) &&
                Objects.equals(bankShortName, that.bankShortName) &&
                Objects.equals(bankBranchCode, that.bankBranchCode) &&
                Objects.equals(bic, that.bic) &&
                Objects.equals(pic, that.pic) &&
                Objects.equals(bin, that.bin) &&
                Objects.equals(status, that.status) &&
                Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, countryCode, bankCode, bankName, bankShortName, bankBranchCode, bic, pic, bin, status, distinct);
    }

    @Override
    public String toString() {
        return "BankCodesLookupCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (countryCode != null ? "countryCode=" + countryCode + ", " : "") +
                (bankCode != null ? "bankCode=" + bankCode + ", " : "") +
                (bankName != null ? "bankName=" + bankName + ", " : "") +
                (bankShortName != null ? "bankShortName=" + bankShortName + ", " : "") +
                (bankBranchCode != null ? "bankBranchCode=" + bankBranchCode + ", " : "") +
                (bic != null ? "bic=" + bic + ", " : "") +
                (pic != null ? "pic=" + pic + ", " : "") +
                (bin != null ? "bin=" + bin + ", " : "") +
                (status != null ? "status=" + status + ", " : "") +
                (distinct != null ? "distinct=" + distinct + ", " : "") +
                "}";
    }
}