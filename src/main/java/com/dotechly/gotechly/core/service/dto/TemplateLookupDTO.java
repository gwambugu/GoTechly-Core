package com.dotechly.gotechly.core.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class TemplateLookupDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 100)
    private String templateName;

    @Size(max = 60)
    private String contentType;

    // Surfaced to REST clients as text. Core stores it as a BLOB; the mapper
    // converts to/from UTF-8 bytes. Templates are text bodies (XML/JSON), so a
    // textarea in the admin UI round-trips them safely.
    private String reqTemplate;

    // Free-form JSON object: Map<String,String> in Java, JSON in the CLOB
    // column (same treatment as Channels/BankCodesLookup additionalFields).
    private Map<String, String> additionalFields;

    @Size(max = 200)
    private String url;

    @Size(max = 50)
    private String additionalHeader1;

    @Size(max = 50)
    private String additionalHeader2;

    @Size(max = 300)
    private String statusExp;

    @Size(max = 300)
    private String tranIdExp;

    @Size(max = 300)
    private String errorExp;

    @Size(max = 300)
    private String timeOutExp;

    @Size(max = 100)
    private String providerCode;

    @Size(max = 100)
    private String param1;

    @Size(max = 100)
    private String param2;

    @Size(max = 100)
    private String modifiedBy;

    private Instant modifyDate;

    // hm is intentionally write-only at the Jackson layer: the Gateway and any
    // other REST client will never see this field in a response payload, but
    // Core internals (and admin tooling that sends it explicitly) can still
    // populate it on writes. The Liquibase changeset keeps the column in the
    // DB; the maker-checker flow in PendingChangeService preserves the
    // existing DB value on UPDATE so an approval never blanks it.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 200)
    private String hm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getReqTemplate() { return reqTemplate; }
    public void setReqTemplate(String reqTemplate) { this.reqTemplate = reqTemplate; }

    public Map<String, String> getAdditionalFields() { return additionalFields; }
    public void setAdditionalFields(Map<String, String> additionalFields) { this.additionalFields = additionalFields; }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateLookupDTO that = (TemplateLookupDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TemplateLookupDTO{" +
            "id=" + id +
            ", templateName='" + templateName + '\'' +
            ", providerCode='" + providerCode + '\'' +
            '}';
    }
}
