package com.dotechly.gotechly.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "PARTNER_CONFIGS")
public class PartnerConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "PARTNER_ID", nullable = false)
    private Long partnerId;

    @NotNull
    @Size(max = 200)
    @Column(name = "PARAM_NAME", length = 200, nullable = false)
    private String paramName;

    @Size(max = 500)
    @Column(name = "PARAM_VALUE", length = 500)
    private String paramValue;

    @Size(max = 500)
    @Column(name = "PARAM_DESCRIPTION", length = 500)
    private String paramDescription;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }

    public String getParamName() { return paramName; }
    public void setParamName(String paramName) { this.paramName = paramName; }

    public String getParamValue() { return paramValue; }
    public void setParamValue(String paramValue) { this.paramValue = paramValue; }

    public String getParamDescription() { return paramDescription; }
    public void setParamDescription(String paramDescription) { this.paramDescription = paramDescription; }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartnerConfig that = (PartnerConfig) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "PartnerConfig{" +
            "id=" + getId() +
            ", partnerId=" + getPartnerId() +
            ", paramName='" + getParamName() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }
}
