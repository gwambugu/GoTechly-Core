package com.dotechly.gotechly.core.service.filter;

import java.util.Objects;

public class RangeFilter<FIELD_TYPE extends Comparable<? super FIELD_TYPE>> extends Filter<FIELD_TYPE> {
    private static final long serialVersionUID = 1L;
    private FIELD_TYPE greaterThan;
    private FIELD_TYPE lessThan;
    private FIELD_TYPE greaterThanOrEqual;
    private FIELD_TYPE lessThanOrEqual;

    public RangeFilter() {
    }

    public RangeFilter(RangeFilter<FIELD_TYPE> filter) {
        super(filter);
        this.greaterThan = filter.greaterThan;
        this.lessThan = filter.lessThan;
        this.greaterThanOrEqual = filter.greaterThanOrEqual;
        this.lessThanOrEqual = filter.lessThanOrEqual;
    }

    public FIELD_TYPE getGreaterThan() {
        return this.greaterThan;
    }

    public void setGreaterThan(FIELD_TYPE greaterThan) {
        this.greaterThan = greaterThan;
    }

    public FIELD_TYPE getLessThan() {
        return this.lessThan;
    }

    public void setLessThan(FIELD_TYPE lessThan) {
        this.lessThan = lessThan;
    }

    public FIELD_TYPE getGreaterThanOrEqual() {
        return this.greaterThanOrEqual;
    }

    public void setGreaterThanOrEqual(FIELD_TYPE greaterThanOrEqual) {
        this.greaterThanOrEqual = greaterThanOrEqual;
    }

    public FIELD_TYPE getLessThanOrEqual() {
        return this.lessThanOrEqual;
    }

    public void setLessThanOrEqual(FIELD_TYPE lessThanOrEqual) {
        this.lessThanOrEqual = lessThanOrEqual;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            if (!super.equals(o)) {
                return false;
            } else {
                RangeFilter<?> that = (RangeFilter)o;
                return Objects.equals(this.greaterThan, that.greaterThan) && Objects.equals(this.lessThan, that.lessThan) && Objects.equals(this.greaterThanOrEqual, that.greaterThanOrEqual) && Objects.equals(this.lessThanOrEqual, that.lessThanOrEqual);
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{super.hashCode(), this.greaterThan, this.lessThan, this.greaterThanOrEqual, this.lessThanOrEqual});
    }

    public String toString() {
        return this.getFilterName() + " [" + (this.getGreaterThan() != null ? "greaterThan=" + String.valueOf(this.getGreaterThan()) + ", " : "") + (this.getLessThan() != null ? "lessThan=" + String.valueOf(this.getLessThan()) + ", " : "") + (this.getGreaterThanOrEqual() != null ? "greaterThanOrEqual=" + String.valueOf(this.getGreaterThanOrEqual()) + ", " : "") + (this.getLessThanOrEqual() != null ? "lessThanOrEqual=" + String.valueOf(this.getLessThanOrEqual()) + ", " : "") + (this.getEquals() != null ? "equals=" + String.valueOf(this.getEquals()) + ", " : "") + (this.getNotEquals() != null ? "notEquals=" + String.valueOf(this.getNotEquals()) + ", " : "") + (this.getIn() != null ? "in=" + String.valueOf(this.getIn()) + ", " : "") + (this.getNotIn() != null ? "notIn=" + String.valueOf(this.getNotIn()) + ", " : "") + (this.getSpecified() != null ? "specified=" + String.valueOf(this.getSpecified()) : "") + "]";
    }

    protected String getFilterName() {
        return "RangeFilter";
    }
}
