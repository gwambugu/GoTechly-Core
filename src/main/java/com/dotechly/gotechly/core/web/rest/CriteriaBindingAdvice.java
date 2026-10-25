package com.dotechly.gotechly.core.web.rest;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Global binder configuration for criteria-based query parameter binding.
 *
 * Spring Boot 4 / Spring Framework 7 now throws MethodArgumentNotValidException
 * on binding type mismatches (previously silently ignored). This causes 400 errors
 * when HTTP headers (e.g., User-Agent) collide with criteria field names (e.g., userAgent)
 * and the plain String value cannot convert to a Filter type.
 *
 * Fix: restrict binding to nested filter properties only (e.g., requestId.equals,
 * eventType.contains) and the top-level 'distinct' boolean. Plain top-level field
 * names that could collide with headers or Pageable params are rejected.
 */
@ControllerAdvice
public class CriteriaBindingAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAutoGrowNestedPaths(true);
        binder.setAllowedFields("*.*", "distinct");
    }
}
