package com.dotechly.gotechly.core.domain;

import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.sql.Types;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "template_lookup")
public class TemplateLookup implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "template_name", length = 100, nullable = false, unique = true)
    private String templateName;

    @Size(max = 60)
    @Column(name = "content_type", length = 60)
    private String contentType;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "req_template")
    private String reqTemplate;

    @Size(max = 200)
    @Column(name = "url", length = 200)
    private String url;

    @Size(max = 50)
    @Column(name = "additional_header_1", length = 50)
    private String additionalHeader1;

    @Size(max = 50)
    @Column(name = "additional_header_2", length = 50)
    private String additionalHeader2;

    @Size(max = 300)
    @Column(name = "status_exp", length = 300)
    private String statusExp;

    @Size(max = 300)
    @Column(name = "tran_id_exp", length = 300)
    private String tranIdExp;

    @Size(max = 300)
    @Column(name = "error_exp", length = 300)
    private String errorExp;

    @Size(max = 300)
    @Column(name = "time_out_exp", length = 300)
    private String timeOutExp;

    @Size(max = 100)
    @Column(name = "provider_code", length = 100)
    private String providerCode;

    @Size(max = 100)
    @Column(name = "param_1", length = 100)
    private String param1;

    @Size(max = 100)
    @Column(name = "param_2", length = 100)
    private String param2;

    @Size(max = 100)
    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Column(name = "modify_date")
    private Instant modifyDate;

    @Size(max = 200)
    @Column(name = "hm", length = 200)
    private String hm;

    /**
     * Serialised as JSON in the CLOB column and surfaced as a Map<String,String>
     * to Java callers via {@link JsonMapConverter}. The {@code @JdbcTypeCode(LONGVARCHAR)}
     * binds it as a plain long string, which maps to TEXT on PostgreSQL
     * (matches the same wiring used by {@code Channels.additionalFields} and
     * {@code BankCodesLookup.additionalFields}).
     */
    @Convert(converter = JsonMapConverter.class)
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "additional_fields")
    private Map<String, String> additionalFields;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getReqTemplate() { return reqTemplate; }
    public void setReqTemplate(String reqTemplate) { this.reqTemplate = reqTemplate; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAdditionalHeader1() { return additionalHeader1; }
    public void setAdditionalHeader1(String additionalHeader1) { this.additionalHeader1 = additionalHeader1; }

    public String getAdditionalHeader2() { return additionalHeader2; }
    public void setAdditionalHeader2(String additionalHeader2) { this.additionalHeader2 = additionalHeader2; }

    public String getStatusExp() { return statusExp; }
    public void setStatusExp(String statusExp) { this.statusExp = statusExp; }

    public String getTranIdExp() { return tranIdExp; }
    public void setTranIdExp(String tranIdExp) { this.tranIdExp = tranIdExp; }

    public String getErrorExp() { return errorExp; }
    public void setErrorExp(String errorExp) { this.errorExp = errorExp; }

    public String getTimeOutExp() { return timeOutExp; }
    public void setTimeOutExp(String timeOutExp) { this.timeOutExp = timeOutExp; }

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }

    public String getParam1() { return param1; }
    public void setParam1(String param1) { this.param1 = param1; }

    public String getParam2() { return param2; }
    public void setParam2(String param2) { this.param2 = param2; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    public Instant getModifyDate() { return modifyDate; }
    public void setModifyDate(Instant modifyDate) { this.modifyDate = modifyDate; }

    public String getHm() { return hm; }
    public void setHm(String hm) { this.hm = hm; }

    public Map<String, String> getAdditionalFields() { return additionalFields; }
    public void setAdditionalFields(Map<String, String> additionalFields) { this.additionalFields = additionalFields; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateLookup that = (TemplateLookup) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "TemplateLookup{" +
            "id=" + id +
            ", templateName='" + templateName + '\'' +
            ", contentType='" + contentType + '\'' +
            ", url='" + url + '\'' +
            ", providerCode='" + providerCode + '\'' +
            ", modifiedBy='" + modifiedBy + '\'' +
            ", modifyDate=" + modifyDate +
            '}';
    }
}
