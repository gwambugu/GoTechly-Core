package com.dotechly.gotechly.core.service.filter;

import java.time.Instant;

public class InstantFilter extends RangeFilter<Instant> {
    private static final long serialVersionUID = 1L;

    public InstantFilter() {
    }

    public InstantFilter(InstantFilter filter) {
        super(filter);
    }
}
