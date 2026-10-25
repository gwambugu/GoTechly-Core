package com.dotechly.gotechly.core.service.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Filter<FIELD_TYPE> implements Serializable {

    private static final long serialVersionUID = 1L;
    private FIELD_TYPE equals;
    private FIELD_TYPE notEquals;
    private List<FIELD_TYPE> in;
    private List<FIELD_TYPE> notIn;
    private Boolean specified;

    public Filter() {}

    public Filter(Filter<FIELD_TYPE> filter) {
        this.equals = filter.equals;
        this.notEquals = filter.notEquals;
        this.in = filter.in == null ? null : new ArrayList<>(filter.in);
        this.notIn = filter.notIn == null ? null : new ArrayList<>(filter.notIn);
        this.specified = filter.specified;
    }

    public FIELD_TYPE getEquals() {
        return this.equals;
    }

    public void setEquals(FIELD_TYPE equals) {
        this.equals = equals;
    }

    public FIELD_TYPE getNotEquals() {
        return this.notEquals;
    }

    public void setNotEquals(FIELD_TYPE notEquals) {
        this.notEquals = notEquals;
    }

    public List<FIELD_TYPE> getIn() {
        return this.in;
    }

    public void setIn(List<FIELD_TYPE> in) {
        this.in = in;
    }

    public List<FIELD_TYPE> getNotIn() {
        return this.notIn;
    }

    public void setNotIn(List<FIELD_TYPE> notIn) {
        this.notIn = notIn;
    }

    public Boolean getSpecified() {
        return this.specified;
    }

    public void setSpecified(Boolean specified) {
        this.specified = specified;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Filter<?> filter = (Filter)o;
            return Objects.equals(this.equals, filter.equals) && Objects.equals(this.notEquals, filter.notEquals) && Objects.equals(this.in, filter.in) && Objects.equals(this.notIn, filter.notIn) && Objects.equals(this.specified, filter.specified);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.equals, this.notEquals, this.in, this.notIn, this.specified});
    }

    public String toString() {
        return this.getFilterName() + " [" + (this.getEquals() != null ? "equals=" + String.valueOf(this.getEquals()) + ", " : "") + (this.getNotEquals() != null ? "notEquals=" + String.valueOf(this.getNotEquals()) + ", " : "") + (this.getIn() != null ? "in=" + String.valueOf(this.getIn()) + ", " : "") + (this.getNotIn() != null ? "notIn=" + String.valueOf(this.getNotIn()) + ", " : "") + (this.getSpecified() != null ? "specified=" + String.valueOf(this.getSpecified()) : "") + "]";
    }

    protected String getFilterName() {
        return "Filter";
    }
}
