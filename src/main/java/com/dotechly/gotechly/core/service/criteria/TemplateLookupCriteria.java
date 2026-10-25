package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.service.filter.InstantFilter;
import com.dotechly.gotechly.core.service.filter.LongFilter;
import com.dotechly.gotechly.core.service.filter.StringFilter;
import java.io.Serializable;
import java.util.Objects;

public class TemplateLookupCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;
    private StringFilter templateName;
    private StringFilter contentType;
    private StringFilter url;
    private StringFilter providerCode;
    private StringFilter param1;
    private StringFilter param2;
    private StringFilter modifiedBy;
    private InstantFilter modifyDate;
    private Boolean distinct;

    public TemplateLookupCriteria() {}

    public TemplateLookupCriteria(TemplateLookupCriteria other) {
        this.id = other.id;
        this.templateName = other.templateName;
        this.contentType = other.contentType;
        this.url = other.url;
        this.providerCode = other.providerCode;
        this.param1 = other.param1;
        this.param2 = other.param2;
        this.modifiedBy = other.modifiedBy;
        this.modifyDate = other.modifyDate;
        this.distinct = other.distinct;
    }

    public LongFilter getId() { return id; }
    public void setId(LongFilter id) { this.id = id; }

    public StringFilter getTemplateName() { return templateName; }
    public void setTemplateName(StringFilter templateName) { this.templateName = templateName; }

    public StringFilter getContentType() { return contentType; }
    public void setContentType(StringFilter contentType) { this.contentType = contentType; }

    public StringFilter getUrl() { return url; }
    public void setUrl(StringFilter url) { this.url = url; }

    public StringFilter getProviderCode() { return providerCode; }
    public void setProviderCode(StringFilter providerCode) { this.providerCode = providerCode; }

    public StringFilter getParam1() { return param1; }
    public void setParam1(StringFilter param1) { this.param1 = param1; }

    public StringFilter getParam2() { return param2; }
    public void setParam2(StringFilter param2) { this.param2 = param2; }

    public StringFilter getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(StringFilter modifiedBy) { this.modifiedBy = modifiedBy; }

    public InstantFilter getModifyDate() { return modifyDate; }
    public void setModifyDate(InstantFilter modifyDate) { this.modifyDate = modifyDate; }

    public Boolean getDistinct() { return distinct; }
    public void setDistinct(Boolean distinct) { this.distinct = distinct; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateLookupCriteria that = (TemplateLookupCriteria) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(templateName, that.templateName) &&
            Objects.equals(contentType, that.contentType) &&
            Objects.equals(url, that.url) &&
            Objects.equals(providerCode, that.providerCode) &&
            Objects.equals(param1, that.param1) &&
            Objects.equals(param2, that.param2) &&
            Objects.equals(modifiedBy, that.modifiedBy) &&
            Objects.equals(modifyDate, that.modifyDate) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, templateName, contentType, url, providerCode,
                param1, param2, modifiedBy, modifyDate, distinct);
    }

    @Override
    public String toString() {
        return "TemplateLookupCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (templateName != null ? "templateName=" + templateName + ", " : "") +
            (contentType != null ? "contentType=" + contentType + ", " : "") +
            (url != null ? "url=" + url + ", " : "") +
            (providerCode != null ? "providerCode=" + providerCode + ", " : "") +
            (param1 != null ? "param1=" + param1 + ", " : "") +
            (param2 != null ? "param2=" + param2 + ", " : "") +
            (modifiedBy != null ? "modifiedBy=" + modifiedBy + ", " : "") +
            (modifyDate != null ? "modifyDate=" + modifyDate + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            "}";
    }
}
