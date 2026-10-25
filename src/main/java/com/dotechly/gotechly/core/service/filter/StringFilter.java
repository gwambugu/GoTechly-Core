package com.dotechly.gotechly.core.service.filter;

import java.util.Objects;

public class StringFilter extends Filter<String> {
    private static final long serialVersionUID = 1L;
    private String contains;
    private String doesNotContain;

    public StringFilter() {
    }

    public StringFilter(StringFilter filter) {
        super(filter);
        this.contains = filter.contains;
        this.doesNotContain = filter.doesNotContain;
    }

    public String getContains() {
        return this.contains;
    }

    public void setContains(String contains) {
        this.contains = contains;
    }

    public String getDoesNotContain() {
        return this.doesNotContain;
    }

    public void setDoesNotContain(String doesNotContain) {
        this.doesNotContain = doesNotContain;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            if (!super.equals(o)) {
                return false;
            } else {
                StringFilter that = (StringFilter)o;
                return Objects.equals(this.contains, that.contains) && Objects.equals(this.doesNotContain, that.doesNotContain);
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{super.hashCode(), this.contains, this.doesNotContain});
    }

    public String toString() {
        return this.getFilterName() + " [" + (this.getContains() != null ? "contains=" + this.getContains() + ", " : "") + (this.getDoesNotContain() != null ? "doesNotContain=" + this.getDoesNotContain() + ", " : "") + (this.getEquals() != null ? "equals=" + this.getEquals() + ", " : "") + (this.getNotEquals() != null ? "notEquals=" + this.getNotEquals() + ", " : "") + (this.getIn() != null ? "in=" + String.valueOf(this.getIn()) + ", " : "") + (this.getNotIn() != null ? "notIn=" + String.valueOf(this.getNotIn()) + ", " : "") + (this.getSpecified() != null ? "specified=" + String.valueOf(this.getSpecified()) : "") + "]";
    }

    protected String getFilterName() {
        return "StringFilter";
    }
}
