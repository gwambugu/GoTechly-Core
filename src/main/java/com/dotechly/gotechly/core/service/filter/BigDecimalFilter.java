package com.dotechly.gotechly.core.service.filter;

import java.math.BigDecimal;

public class BigDecimalFilter extends RangeFilter<BigDecimal> {
    private static final long serialVersionUID = 1L;

    public BigDecimalFilter() {
    }

    public BigDecimalFilter(BigDecimalFilter filter) {
        super(filter);
    }
}
