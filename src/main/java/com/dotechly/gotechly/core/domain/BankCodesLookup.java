package com.dotechly.gotechly.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.sql.Types;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "bank_codes_lookup")
public class BankCodesLookup implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @Size(max = 5)
    @Column(name = "country_code", length = 5)
    private String countryCode;

    @Size(max = 5)
    @Column(name = "bank_code", length = 5)
    private String bankCode;

    @Size(max = 100)
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Size(max = 30)
    @Column(name = "bank_short_name", length = 30)
    private String bankShortName;

    @Size(max = 5)
    @Column(name = "bank_branch_code", length = 5)
    private String bankBranchCode;

    @Size(max = 50)
    @Column(name = "bic", length = 50)
    private String bic;

    @Size(max = 20)
    @Column(name = "pic", length = 20)
    private String pic;

    @Size(max = 20)
    @Column(name = "bin", length = 20)
    private String bin;

    /**
     * Serialised as JSON in the CLOB column and surfaced as a Map<String,String>
     * to Java callers via {@link JsonMapConverter}. The {@code @JdbcTypeCode(LONGVARCHAR)}
     * binds it as a plain long string, which maps to TEXT on PostgreSQL
     * (matches the same wiring used by {@code Channels.additionalFields}).
     */
    @Convert(converter = JsonMapConverter.class)
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "additional_fields")
    private Map<String, String> additionalFields;

    @Size(max = 20)
    @Column(name = "status", length = 20)
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
        BankCodesLookup that = (BankCodesLookup) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BankCodesLookup{" +
                "id=" + id +
                ", countryCode='" + countryCode + '\'' +
                ", bankCode='" + bankCode + '\'' +
                ", bankName='" + bankName + '\'' +
                ", bankShortName='" + bankShortName + '\'' +
                ", bankBranchCode='" + bankBranchCode + '\'' +
                ", bic='" + bic + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
